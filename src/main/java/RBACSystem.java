import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RBACSystem {
    UserManager userManager;
    RoleManager roleManager;
    AssignmentManager assignmentManager;
    CommandParser commandParser;
    AuditLog auditLog;
    String currentUser;
    ExecutorService executorService;

    public UserManager getUserManager(){
        return this.userManager;
    }

    public RoleManager getRoleManager(){
        return this.roleManager;
    }

    public AssignmentManager getAssignmentManager(){
        return this.assignmentManager;
    }

    public AuditLog getAuditLog(){
        return this.auditLog;
    }

    public ExecutorService getExecutorService(){
        return this.executorService;
    }

    void setCurrentUser(String username){
        this.currentUser = username;
    }

    String getCurrentUser(){
        return this.currentUser;
    }

    void initialize(){
        userManager = new UserManager();
        roleManager = new RoleManager(null);
        assignmentManager = new AssignmentManager(userManager, roleManager);
        roleManager.setAssignmentManager(assignmentManager);
        auditLog = new AuditLog();
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        commandParser = new CommandParser();
        CommandRegistry.registerAllCommands(commandParser);

        User testAdmin = User.create("admin_user", "Admin Full Name", "admin@example.com");
        userManager.add(testAdmin);

        Permission readPermission = new Permission("READ", "users", "Can read users");
        Permission writePermission = new Permission("WRITE", "users", "Can write reports");
        Permission deletePermission = new Permission("DELETE", "users", "Can delete settings");

        Role adminRole = new Role("Admin", "Admin role");
        adminRole.addPermission(readPermission);
        adminRole.addPermission(writePermission);
        adminRole.addPermission(deletePermission);

        Role managerRole = new Role("Manager", "Manager role");
        managerRole.addPermission(readPermission);
        managerRole.addPermission(writePermission);

        Role viewerRole = new Role("Viewer", "Viewer role");
        viewerRole.addPermission(readPermission);

        roleManager.add(adminRole);
        roleManager.add(managerRole);
        roleManager.add(viewerRole);

        AssignmentMetadata meta = AssignmentMetadata.now("system", "Initial admin assignment");
        PermanentAssignment testAssignment = new PermanentAssignment(testAdmin, adminRole, meta);
        assignmentManager.add(testAssignment);

        this.currentUser = "admin_user";
    }

    String generateStatistics(){
        return "Current administrator: " + currentUser + "\n" +
                "Total users: " + userManager.count() + "\n" +
                "Total roles: " + roleManager.count() + "\n" +
                "Role details:\n" +
                "Total assignments: " + assignmentManager.count() + "\n";
    }

    void shutdownExecutorService(){
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (auditLog != null) {
            auditLog.shutdown();
        }
    }
}
