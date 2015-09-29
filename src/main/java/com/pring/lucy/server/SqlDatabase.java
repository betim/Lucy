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

public class SqlDatabase {
  protected SqlDatabase() {
    
  }
  
  public Connection getConnection() throws SQLException {
    return Server.ds.getConnection();
  }

  public String selectCell(String sql) {
    return selectCell(sql, new Object[] {});
  }
  
  public String selectCell(String sql, Object... args) {
    StringBuilder buffer = new StringBuilder();

    Connection c = null;
    PreparedStatement stmt = null;
    try {
      c = getConnection();
      stmt = c.prepareStatement(sql);
      setArgs(stmt, args);

      ResultSet rs = stmt.executeQuery();
      
      if (rs.next())
        buffer.append(rs.getString(1)).append('\n');
      
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
  
  public ResultSetIterator select(String query) {
    return select(query, new Object[] {});
  }
  
  public ResultSetIterator select(String query, Object... args) {
    Connection c = null;
    PreparedStatement stmt = null;
    try {
      c = getConnection();
      stmt = c.prepareStatement(query);
      setArgs(stmt, args);
      
      return new ResultSetIterator(stmt.executeQuery(), stmt, c);
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    return new ResultSetIterator();
  }
  
  public int update(String query, Object... args) {
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

  public long insert(String query, Object... args) {
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
    
    return result;
  }
  
  private void setArgs(PreparedStatement stmt, Object... args) {
    for (int i = 0; i < args.length; i++) {
      try {
        stmt.setObject(i + 1, args[i]);
      } catch (SQLException e) {
        e.printStackTrace();
      } 
    }
  }
  
  public static class ResultSetIterator implements Iterable<ResultSet> {
    private Connection conn;
    private Statement stmt;
    private ResultSet rs;

    private List<String> columns = new ArrayList<String>();
    
    private ResultSetIterator(ResultSet rs, Statement stmt, Connection conn) {
      this.conn = conn;
      this.stmt = stmt;
      this.rs = rs;

      try {
        ResultSetMetaData cols = rs.getMetaData();
        for (int i = 1; i <= cols.getColumnCount(); i++) {
          columns.add(cols.getColumnName(i));
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    
    private ResultSetIterator() { }
    
    public List<String> columns() {
      return columns;
    }
    
    public void close() {
      try {
        rs.close();
        stmt.close();
        conn.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    
    @Override
    public Iterator<ResultSet> iterator() {
      return new Iterator<ResultSet>() {
        @Override
        public boolean hasNext() {
          boolean hasNext = false;
          
          try {
            hasNext = rs.next();
          } catch (Exception e) {
            e.printStackTrace();
            return hasNext;
          }
          
          if (!hasNext)
            close();
          
          return hasNext;
        }

        @Override
        public ResultSet next() {
          return rs;
        }
      };
    }
  }
}
