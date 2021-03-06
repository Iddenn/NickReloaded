package fr.antoinerochas.nickreloaded.core.storage.mysql;


import fr.antoinerochas.nickreloaded.core.storage.core.DatabaseImpl;

import java.sql.*;

public class MySQLDatabase
        implements DatabaseImpl
{
    private Connection connection;
    private String host;
    private String user;
    private String password;

    private boolean connected;

    public MySQLDatabase(final String host, final int port, final String user, final String password, final String database)
    {
        this.host = "jdbc:mysql://" + host + ":" + port + "/" + database;
        this.user = user;
        this.password = password;
    }

    @Override
    public Table getTable(String name)
    {
        return new Table(this,
                         name);
    }

    @Override
    public void connect()
    {
        if (connected)
        {
            return;
        }

        try
        {
            Class.forName("com.mysql.jdbc.Driver").newInstance();

            this.connection = DriverManager.getConnection(host,
                                                          user,
                                                          password);
            this.connected = true;
        }
        catch (SQLException | IllegalAccessException | InstantiationException | ClassNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void close()
    {
        if (! connected)
        {
            return;
        }

        try
        {
            connection.close();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public String getHost()
    {
        return host;
    }

    public String getUser()
    {
        return user;
    }

    public String getPassword()
    {
        return password;
    }

    @Override
    public Connection getConnection()
    {
        return connection;
    }

    @Override
    public void setConnection(Connection connection)
    {
        this.connection = connection;
    }

    @Override
    public boolean isConnected()
    {
        return connected;
    }
}
