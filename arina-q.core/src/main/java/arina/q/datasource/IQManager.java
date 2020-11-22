package arina.q.datasource;

public interface IQManager
{
    void install(String dbaServerName, String dbaServerPort, String dbaDatabaseName, String dbaLogin, String dbaPassword, String systemName, String systemPassword, String systemStorage) throws Exception;
    void drop(String dbaServerName, String dbaServerPort, String dbaDatabaseName, String dbaLogin, String dbaPassword, String systemName, String systemStorage) throws Exception;
}
