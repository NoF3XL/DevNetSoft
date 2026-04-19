import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RBACSystem {
    UserManager userManager;
    RoleManager roleManager;
    AssignmentManager assignmentManager;
    CommandParser commandParser;
    AuditLog auditLog;
    String currentUser;
    ExecutorService executorService;
    ScheduledExecutorService scheduledExecutorService;
    private static final int EXPIRATION_CHECK_INTERVAL_SECONDS = 30;

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

    public ScheduledExecutorService getScheduledExecutorService(){
        return this.scheduledExecutorService;
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
        scheduledExecutorService = new ScheduledThreadPoolExecutor(1);
        commandParser = new CommandParser();
        CommandRegistry.registerAllCommands(commandParser);
        scheduledExecutorService.scheduleAtFixedRate(new ExpirationMonitor(),
                EXPIRATION_CHECK_INTERVAL_SECONDS, EXPIRATION_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);

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
        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
            scheduledExecutorService.shutdown();
        }
        if (auditLog != null) {
            auditLog.shutdown();
        }
    }

    private class ExpirationMonitor implements Runnable {
        @Override
        public void run() {
            try {
                List<RoleAssignment> expired = assignmentManager.getExpiredAssignments();
                int total = assignmentManager.count();
                int active = assignmentManager.getActiveAssignments().size();
                int expiredCount = expired.size();
                String details = String.format(
                    "Expired assignments: %d, Active assignments: %d, Total assignments: %d",
                    expiredCount, active, total
                );
                auditLog.log("EXPIRATION_CHECK", "system", "assignments", details);
            } catch (Exception e) {
                System.err.println("ExpirationMonitor error: " + e.getMessage());
            }
        }
    }
}
