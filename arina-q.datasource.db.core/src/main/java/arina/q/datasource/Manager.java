package arina.q.datasource;

import java.util.Scanner;

public class Manager
{
    private static String readLine(String prompt)
    {
        System.out.print(prompt);
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine();
    }

    public static void main(String[] args) throws Exception
    {

        String operation = readLine("Choose operation (install|drop): ");
        if("install".equalsIgnoreCase(operation) || "drop".equalsIgnoreCase(operation))
        {
            String driverClass = readLine("Enter driver class name (FQN): ");
            Class<?> clazz = Class.forName(driverClass);
            IQManager qmanager = (IQManager) clazz.getDeclaredConstructor().newInstance();

            String dbaServerName = readLine("Enter server name or ip: ");
            String dbaServerPort = readLine("Enter server port: ");
            String dbaDatabaseName = readLine("Enter database name: ");
            String dbaLogin = readLine("Enter dba login: ");
            String dbaPassword = readLine("Enter dba password: ");
            String systemName = readLine("Enter system id: ");
            String systemStorage = readLine("Enter system storage: ");
            if("install".equalsIgnoreCase(operation))
            {
                String systemPassword = readLine("Enter system password: ");
                qmanager.install(dbaServerName, dbaServerPort, dbaDatabaseName, dbaLogin, dbaPassword, systemName, systemPassword, systemStorage);
            }
            else
                qmanager.drop(dbaServerName, dbaServerPort, dbaDatabaseName, dbaLogin, dbaPassword, systemName, systemStorage);
        }
        else
        {
            System.out.println("Nothing to do.");
        }
    }
}
