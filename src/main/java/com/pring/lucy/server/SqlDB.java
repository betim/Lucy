package com.pring.lucy.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.sql.rowset.CachedRowSet;

import com.sun.rowset.CachedRowSetImpl;
 
public class SqlDB {
  protected SqlDB() { }
  
  public static Connection getConnection() throws SQLException {
    return Server.ds.getConnection();
  }

  public static String selectCell(String sql, Object... args) {
    StringBuilder buffer = new StringBuilder();

    Connection c = null;
    PreparedStatement stmt = null;
    try {
      c = getConnection();
      stmt = c.prepareStatement(sql);
      setArgs(stmt, args);

      ResultSet rs = stmt.executeQuery();
      
      if (rs.next())
        buffer.append(rs.getString(1));
      
      rs.close();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        stmt.close();
        c.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    
    return buffer.toString();
  }

  public static CachedRowSet query(String query, Object... args) {
    CachedRowSet result = null;

    Connection connection = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;

    try {
      result = new CachedRowSetImpl();
      
      connection = getConnection();
      statement = connection.prepareStatement(query);
      setArgs(statement, args);
      resultSet = statement.executeQuery();
      
      result.populate(resultSet);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        resultSet.close();
        statement.close();
        connection.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }

    return result;
  }
  
  public static ResultSetIterator select(String query, Object... args) {
    return new ResultSetIterator(query, args);
  }
  
  public static long insert(String query, Object... args) {
    long result = -1;

    Connection c = null;
    PreparedStatement stmt = null;
    try {
      c = getConnection();
      stmt = c.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
      setArgs(stmt, args);

      stmt.executeUpdate();
      ResultSet rs = stmt.getGeneratedKeys();
      if (rs.next()) {
        result = rs.getLong(1);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        stmt.close();
        c.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    
    return result;
  }

  public static int update(String query, Object... args) {
    int result = 0;
    
    Connection c = null;
    PreparedStatement stmt = null;
    try {
      c = getConnection(); 
      stmt = c.prepareStatement(query);
      setArgs(stmt, args);
      
      result = stmt.executeUpdate();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        stmt.close();
        c.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    
    return result;
  }
  
  public static int delete(String query, Object... args) {
    return update(query, args);
  }
  
  private static void setArgs(PreparedStatement stmt, Object... args) {
    for (int i = 0; i < args.length; i++) {
      try {
        stmt.setObject(i + 1, args[i]);
      } catch (SQLException e) {
        e.printStackTrace();
      } 
    }
  }
  
  public static class ResultSetIterator implements Iterable<ResultSet> {
    private List<String> columns = new ArrayList<String>();
    
    private Connection connection;
    private PreparedStatement statement;
    private ResultSet resultSet;

    private ResultSetIterator(String query, Object[] args) {
      try {
        connection = getConnection();
        
        statement = connection.prepareStatement(query);
        setArgs(statement, args);
        resultSet = statement.executeQuery();
      } catch (Exception e) {
        e.printStackTrace();
      }
      
      try {
        ResultSetMetaData cols = resultSet.getMetaData();
        for (int i = 1; i <= cols.getColumnCount(); i++) {
          columns.add(cols.getColumnName(i));
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    
    public List<String> columns() {
      return columns;
    }
    
    @Override
    public Iterator<ResultSet> iterator() {
      return new Iterator<ResultSet>() {
        @Override
        public boolean hasNext() {
          boolean hasNext = false;

          try {
            hasNext = resultSet.next();            
          } catch (Exception e) {
            e.printStackTrace();
          }
          
          if (hasNext == false)
            close();
          
          return hasNext;
        }

        @Override
        public ResultSet next() {
          return resultSet;
        }
      };
    }
    
    public void close() {
      try {
        resultSet.close();
        statement.close();
        connection.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }
}
