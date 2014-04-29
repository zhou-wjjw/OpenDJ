/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at legal-notices/CDDLv1_0.txt
 * or http://forgerock.org/license/CDDLv1.0.html.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at legal-notices/CDDLv1_0.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2006-2009 Sun Microsystems, Inc.
 *      Portions Copyright 2011-2014 ForgeRock AS
 */
package org.opends.server.replication.server.changelog.je;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opends.messages.Message;
import org.opends.messages.MessageBuilder;
import org.opends.server.loggers.debug.DebugTracer;
import org.opends.server.replication.server.ChangelogState;
import org.opends.server.replication.server.ReplicationServer;
import org.opends.server.replication.server.changelog.api.ChangelogException;
import org.opends.server.types.DN;
import org.opends.server.types.DirectoryException;

import com.sleepycat.je.*;

import static com.sleepycat.je.EnvironmentConfig.*;
import static com.sleepycat.je.OperationStatus.*;

import static org.opends.messages.JebMessages.*;
import static org.opends.messages.ReplicationMessages.*;
import static org.opends.server.loggers.ErrorLogger.*;
import static org.opends.server.loggers.debug.DebugLogger.*;
import static org.opends.server.util.StaticUtils.*;

/**
 * This class represents a DB environment that acts as a factory for
 * ReplicationDBs.
 */
public class ReplicationDbEnv
{
  private Environment dbEnvironment;
  private Database changelogStateDb;
  private final List<Database> allDbs = new CopyOnWriteArrayList<Database>();
  private ReplicationServer replicationServer;
  private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
  private static final String GENERATION_ID_TAG = "GENID";
  private static final String FIELD_SEPARATOR = " ";
  /** The tracer object for the debug logger. */
  private static final DebugTracer TRACER = getTracer();

  /**
   * Initialize this class.
   * Creates Db environment that will be used to create databases.
   * It also reads the currently known databases from the "changelogstate"
   * database.
   * @param path Path where the backing files must be created.
   * @param replicationServer the ReplicationServer that creates this
   *                          ReplicationDbEnv.
   * @throws ChangelogException If an Exception occurred that prevented
   *                           the initialization to happen.
   */
  public ReplicationDbEnv(String path, ReplicationServer replicationServer)
      throws ChangelogException
  {
    this.replicationServer = replicationServer;

    try
    {
      dbEnvironment = openJEEnvironment(path);

      /*
       * One database is created to store the update from each LDAP server in
       * the topology. The database "changelogstate" is used to store the list
       * of all the servers that have been seen in the past.
       */
      changelogStateDb = openDatabase("changelogstate");
    }
    catch (RuntimeException e)
    {
      throw new ChangelogException(e);
    }
  }

  /**
   * Open a JE environment.
   * <p>
   * protected so it can be overridden by tests.
   *
   * @param path
   *          the path to the JE environment in the filesystem
   * @return the opened JE environment
   */
  protected Environment openJEEnvironment(String path)
  {
    final EnvironmentConfig envConfig = new EnvironmentConfig();

    /*
     * Create the DB Environment that will be used for all the
     * ReplicationServer activities related to the db
     */
    envConfig.setAllowCreate(true);
    envConfig.setTransactional(true);
    envConfig.setConfigParam(STATS_COLLECT, "false");
    envConfig.setConfigParam(CLEANER_THREADS, "2");
    envConfig.setConfigParam(CHECKPOINTER_HIGH_PRIORITY, "true");
    /*
     * Tests have shown that since the parsing of the Replication log is
     * always done sequentially, it is not necessary to use a large DB cache.
     */
    if (Runtime.getRuntime().maxMemory() > 256 * 1024 * 1024)
    {
      /*
       * If the JVM is reasonably large then we can safely default to bigger
       * read buffers. This will result in more scalable checkpointer and
       * cleaner performance.
       */
      envConfig.setConfigParam(CLEANER_LOOK_AHEAD_CACHE_SIZE, mb(2));
      envConfig.setConfigParam(LOG_ITERATOR_READ_SIZE, mb(2));
      envConfig.setConfigParam(LOG_FAULT_READ_SIZE, kb(4));

      /*
       * The cache size must be bigger in order to accommodate the larger
       * buffers - see OPENDJ-943.
       */
      envConfig.setConfigParam(MAX_MEMORY, mb(16));
    }
    else
    {
      /*
       * Use 5M so that the replication can be used with 64M total for the
       * JVM.
       */
      envConfig.setConfigParam(MAX_MEMORY, mb(5));
    }

    // Since records are always added at the end of the Replication log and
    // deleted at the beginning of the Replication log, this should never
    // cause any deadlock.
    envConfig.setTxnTimeout(0, TimeUnit.SECONDS);
    envConfig.setLockTimeout(0, TimeUnit.SECONDS);

    // Since replication provides durability, we can reduce the DB durability
    // level so that we are immune to application / JVM crashes.
    envConfig.setDurability(Durability.COMMIT_WRITE_NO_SYNC);

    return new Environment(new File(path), envConfig);
  }

  private String kb(int sizeInKb)
  {
    return String.valueOf(sizeInKb * 1024);
  }

  private String mb(int sizeInMb)
  {
    return String.valueOf(sizeInMb * 1024 * 1024);
  }

  /**
   * Open a JE database.
   * <p>
   * protected so it can be overridden by tests.
   *
   * @param databaseName
   *          the databaseName to open
   * @return the opened JE database
   * @throws ChangelogException
   *           if a problem happened opening the database
   * @throws RuntimeException
   *           if a problem happened with the JE database
   */
  protected Database openDatabase(String databaseName)
      throws ChangelogException, RuntimeException
  {
    if (isShuttingDown.get())
    {
      throw new ChangelogException(
          WARN_CANNOT_OPEN_DATABASE_BECAUSE_SHUTDOWN_WAS_REQUESTED.get(
              databaseName, replicationServer.getServerId()));
    }
    final DatabaseConfig dbConfig = new DatabaseConfig();
    dbConfig.setAllowCreate(true);
    dbConfig.setTransactional(true);
    final Database db =
        dbEnvironment.openDatabase(null, databaseName, dbConfig);
    if (isShuttingDown.get())
    {
      closeDB(db);
      throw new ChangelogException(
          WARN_CANNOT_OPEN_DATABASE_BECAUSE_SHUTDOWN_WAS_REQUESTED.get(
              databaseName, replicationServer.getServerId()));
    }
    allDbs.add(db);
    return db;
  }

  /**
   * Read and return the list of known servers from the database.
   *
   * @return the {@link ChangelogState} read from the changelogState DB
   * @throws ChangelogException
   *           if a database problem occurs
   */
  public ChangelogState readChangelogState() throws ChangelogException
  {
    return decodeChangelogState(readWholeState());
  }

  /**
   * Decode the whole changelog state DB.
   *
   * @param wholeState
   *          the whole changelog state DB as a Map.
   *          The Map is only used as a convenient collection of key => data objects 
   * @return the decoded changelog state
   * @throws ChangelogException
   *           if a problem occurred while decoding
   */
  ChangelogState decodeChangelogState(Map<byte[], byte[]> wholeState)
      throws ChangelogException
  {
    try
    {
      final ChangelogState result = new ChangelogState();
      for (Entry<byte[], byte[]> entry : wholeState.entrySet())
      {
        final String stringKey = toString(entry.getKey());
        final String stringData = toString(entry.getValue());

        if (debugEnabled())
        {
          debug("read (key, value)=(" + stringKey + ", " + stringData + ")");
        }

        final String[] str = stringData.split(FIELD_SEPARATOR, 3);
        if (str[0].equals(GENERATION_ID_TAG))
        {
          final long generationId = toLong(str[1]);
          final DN baseDN = DN.decode(str[2]);
          if (debugEnabled())
          {
            debug("has read generationId: baseDN=" + baseDN + " generationId="
                + generationId);
          }
          result.setDomainGenerationId(baseDN, generationId);
        }
        else
        {
          final int serverId = toInt(str[0]);
          final DN baseDN = DN.decode(str[1]);
          if (debugEnabled())
          {
            debug("has read replica: baseDN=" + baseDN + " serverId=" + serverId);
          }
          result.addServerIdToDomain(serverId, baseDN);
        }
      }
      return result;
    }
    catch (DirectoryException e)
    {
      throw new ChangelogException(e.getMessageObject(), e);
    }
  }

  private Map<byte[], byte[]> readWholeState() throws ChangelogException
  {
    DatabaseEntry key = new DatabaseEntry();
    DatabaseEntry data = new DatabaseEntry();
    Cursor cursor = changelogStateDb.openCursor(null, null);

    try
    {
      final Map<byte[], byte[]> results = new LinkedHashMap<byte[], byte[]>();

      OperationStatus status = cursor.getFirst(key, data, LockMode.DEFAULT);
      while (status == OperationStatus.SUCCESS)
      {
        results.put(key.getData(), data.getData());
        status = cursor.getNext(key, data, LockMode.DEFAULT);
      }

      return results;
    }
    catch (RuntimeException e)
    {
      final Message message = ERR_JEB_DATABASE_EXCEPTION.get(e.getMessage());
      throw new ChangelogException(message, e);
    }
    finally
    {
      close(cursor);
    }
  }

  private int toInt(String data) throws ChangelogException
  {
    try
    {
      return Integer.parseInt(data);
    }
    catch (NumberFormatException e)
    {
      // should never happen
      // TODO: i18n
      throw new ChangelogException(Message.raw(
          "replicationServer state database has a wrong format: "
          + e.getLocalizedMessage() + "<" + data + ">"));
    }
  }

  private long toLong(String data) throws ChangelogException
  {
    try
    {
      return Long.parseLong(data);
    }
    catch (NumberFormatException e)
    {
      // should never happen
      // TODO: i18n
      throw new ChangelogException(Message.raw(
          "replicationServer state database has a wrong format: "
          + e.getLocalizedMessage() + "<" + data + ">"));
    }
  }

  private String toString(byte[] data) throws ChangelogException
  {
    try
    {
      return new String(data, "UTF-8");
    }
    catch (UnsupportedEncodingException e)
    {
      // should never happens
      // TODO: i18n
      throw new ChangelogException(Message.raw("need UTF-8 support"));
    }
  }

  /**
   * Converts the string to a UTF8-encoded byte array.
   *
   * @param s
   *          the string to convert
   * @return the byte array representation of the UTF8-encoded string
   */
  static byte[] toBytes(String s)
  {
    try
    {
      return s.getBytes("UTF-8");
    }
    catch (UnsupportedEncodingException e)
    {
      // can't happen
      return null;
    }
  }

  /**
   * Finds or creates the database used to store changes for a replica with the
   * given baseDN and serverId.
   *
   * @param serverId
   *          The server id that identifies the server.
   * @param baseDN
   *          The baseDN that identifies the domain.
   * @param generationId
   *          The generationId associated to this domain.
   * @return the Database.
   * @throws ChangelogException
   *           in case of underlying Exception.
   */
  public Database getOrAddReplicationDB(int serverId, DN baseDN, long generationId)
      throws ChangelogException
  {
    if (debugEnabled())
    {
      debug("ReplicationDbEnv.getOrAddDb(" + serverId + ", " + baseDN + ", "
          + generationId + ")");
    }
    try
    {
      // JNR: redundant info is stored between the key and data down below.
      // It is probably ok since "changelogstate" DB does not receive a high
      // volume of inserts.
      Entry<String, String> replicaEntry = toReplicaEntry(baseDN, serverId);

      // Opens the DB for the changes received from this server on this domain.
      final Database replicaDB = openDatabase(replicaEntry.getKey());

      putInChangelogStateDBIfNotExist(replicaEntry);
      putInChangelogStateDBIfNotExist(toGenIdEntry(baseDN, generationId));
      return replicaDB;
    }
    catch (RuntimeException e)
    {
      throw new ChangelogException(e);
    }
  }

  /**
   * Return an entry to store in the changelog state database representing a
   * replica in the topology.
   *
   * @param baseDN
   *          the replica's baseDN
   * @param serverId
   *          the replica's serverId
   * @return a database entry for the replica
   */
  Entry<String, String> toReplicaEntry(DN baseDN, int serverId)
  {
    final String key = serverId + FIELD_SEPARATOR + baseDN.toNormalizedString();
    return new SimpleImmutableEntry<String, String>(key, key);
  }

  /**
   * Return an entry to store in the changelog state database representing the
   * domain generation id.
   *
   * @param baseDN
   *          the domain's baseDN
   * @param generationId
   *          the domain's generationId
   * @return a database entry for the generationId
   */
  Entry<String, String> toGenIdEntry(DN baseDN, long generationId)
  {
    final String normDn = baseDN.toNormalizedString();
    final String key = GENERATION_ID_TAG + FIELD_SEPARATOR + normDn;
    final String data = GENERATION_ID_TAG + FIELD_SEPARATOR + generationId
        + FIELD_SEPARATOR + normDn;
    return new SimpleImmutableEntry<String, String>(key, data);
  }

  private void putInChangelogStateDBIfNotExist(Entry<String, String> entry)
      throws RuntimeException
  {
    DatabaseEntry key = new DatabaseEntry(toBytes(entry.getKey()));
    DatabaseEntry data = new DatabaseEntry();
    if (changelogStateDb.get(null, key, data, LockMode.DEFAULT) == NOTFOUND)
    {
      Transaction txn = dbEnvironment.beginTransaction(null, null);
      try
      {
        data.setData(toBytes(entry.getValue()));
        if (debugEnabled())
        {
          debug("putting record in the changelogstate Db key=["
              + entry.getKey() + "] value=[" + entry.getValue() + "]");
        }
        changelogStateDb.put(txn, key, data);
        txn.commit(Durability.COMMIT_WRITE_NO_SYNC);
      }
      catch (DatabaseException dbe)
      {
        // Abort the txn and propagate the Exception to the caller
        txn.abort();
        throw dbe;
      }
    }
  }

    /**
     * Creates a new transaction.
     *
     * @return the transaction.
     * @throws ChangelogException in case of underlying exception
     */
    public Transaction beginTransaction() throws ChangelogException
    {
      try
      {
        return dbEnvironment.beginTransaction(null, null);
      }
      catch (RuntimeException e)
      {
        throw new ChangelogException(e);
      }
    }

  /**
   * Shutdown the Db environment.
   */
  public void shutdown()
  {
    isShuttingDown.set(true);
    // CopyOnWriteArrayList iterator never throw ConcurrentModificationException
    // This code rely on openDatabase() to close databases opened concurrently
    // with this code
    final Database[] allDbsCopy = allDbs.toArray(new Database[0]);
    allDbs.clear();
    for (Database db : allDbsCopy)
    {
      closeDB(db);
    }

    try
    {
      dbEnvironment.close();
    }
    catch (DatabaseException e)
    {
      logError(closeDBErrorMessage(null, e));
    }
  }

  private void closeDB(Database db)
  {
    allDbs.remove(db);
    try
    {
      db.close();
    }
    catch (DatabaseException e)
    {
      logError(closeDBErrorMessage(db.getDatabaseName(), e));
    }
  }

  private Message closeDBErrorMessage(String dbName, DatabaseException e)
  {
    if (dbName != null)
    {
      return NOTE_EXCEPTION_CLOSING_DATABASE.get(dbName,
          stackTraceToSingleLineString(e));
    }
    return ERR_ERROR_CLOSING_CHANGELOG_ENV.get(stackTraceToSingleLineString(e));
  }

  /**
   * Clears the provided generationId associated to the provided baseDN from the
   * state Db.
   *
   * @param baseDN
   *          The baseDN for which the generationID must be cleared.
   * @throws ChangelogException
   *           If a database problem happened
   */
  public void clearGenerationId(DN baseDN) throws ChangelogException
  {
    final int unusedGenId = 0;
    deleteFromChangelogStateDB(toGenIdEntry(baseDN, unusedGenId),
        "clearGenerationId(baseDN=" + baseDN + ")");
  }

  /**
   * Clears the provided serverId associated to the provided baseDN from the
   * state Db.
   *
   * @param baseDN
   *          The baseDN for which the serverId must be cleared.
   * @param serverId
   *          The serverId to remove from the Db.
   * @throws ChangelogException
   *           If a database problem happened
   */
  public void clearServerId(DN baseDN, int serverId) throws ChangelogException
  {
    deleteFromChangelogStateDB(toReplicaEntry(baseDN, serverId),
        "clearServerId(baseDN=" + baseDN + " , serverId=" + serverId + ")");
  }

  private void deleteFromChangelogStateDB(Entry<String, ?> entry,
      String methodInvocation) throws ChangelogException
  {
    if (debugEnabled())
    {
      debug(methodInvocation + " starting");
    }

    try
    {
      final DatabaseEntry key = new DatabaseEntry(toBytes(entry.getKey()));
      final DatabaseEntry data = new DatabaseEntry();
      if (changelogStateDb.get(null, key, data, LockMode.DEFAULT) == SUCCESS)
      {
        Transaction txn = dbEnvironment.beginTransaction(null, null);
        try
        {
          changelogStateDb.delete(txn, key);
          txn.commit(Durability.COMMIT_WRITE_NO_SYNC);
          if (debugEnabled())
          {
            debug(methodInvocation + " succeeded");
          }
        }
        catch (RuntimeException dbe)
        {
          // Abort the txn and propagate the Exception to the caller
          txn.abort();
          throw dbe;
        }
      }
      else
      {
        if (debugEnabled())
        {
          debug(methodInvocation + " failed: key=[ " + entry.getKey()
              + "] not found");
        }
      }
    }
    catch (RuntimeException dbe)
    {
      throw new ChangelogException(dbe);
    }
  }

    /**
     * Clears the database.
     *
     * @param db
     *          The database to clear.
     */
    public final void clearDb(Database db)
    {
      String databaseName = db.getDatabaseName();

      // Closing is requested by Berkeley JE before truncate
      db.close();

      Transaction txn = null;
      try
      {
        txn = dbEnvironment.beginTransaction(null, null);
        dbEnvironment.truncateDatabase(txn, databaseName, false);
        txn.commit(Durability.COMMIT_WRITE_NO_SYNC);
        txn = null;
      }
      catch (RuntimeException e)
      {
        MessageBuilder mb = new MessageBuilder();
        mb.append(ERR_ERROR_CLEARING_DB.get(databaseName,
            e.getMessage() + " " +
            stackTraceToSingleLineString(e)));
        logError(mb.toMessage());
      }
      finally
      {
        try
        {
          if (txn != null)
          {
            txn.abort();
          }
        }
        catch(Exception e)
        { /* do nothing */ }
      }
    }

    /**
     * Get or create a db to manage integer change  number associated
     * to multidomain server state.
     * TODO:ECL how to manage compatibility of this db with  new domains
     * added or removed ?
     * @return the retrieved or created db.
     * @throws ChangelogException when a problem occurs.
     */
    public Database getOrCreateCNIndexDB() throws ChangelogException
    {
      try
      {
        // Opens the database for change number associated to this domain.
        // Create it if it does not already exist.
        return openDatabase("draftcndb");
      }
      catch (RuntimeException e)
      {
        throw new ChangelogException(e);
      }
    }

  /**
   * Shuts down replication when an unexpected database exception occurs. Note
   * that we do not expect lock timeouts or txn timeouts because the replication
   * databases are deadlock free, thus all operations should complete
   * eventually.
   *
   * @param e
   *          The unexpected database exception.
   */
  void shutdownOnException(DatabaseException e)
  {
    logError(ERR_CHANGELOG_SHUTDOWN_DATABASE_ERROR
        .get(stackTraceToSingleLineString(e)));
    replicationServer.shutdown();
  }

  private void debug(String message)
  {
    TRACER.debugInfo("In " + replicationServer.getMonitorInstanceName() + ", "
        + message);
  }

}
