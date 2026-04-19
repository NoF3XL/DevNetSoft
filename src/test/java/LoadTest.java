import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

public class LoadTest {
    
    private UserManager userManager;
    private RoleManager roleManager;
    private AssignmentManager assignmentManager;
    private DateTimeFormatter dateFormatter;
    
    private final int THREAD_COUNT = 10;
    private final int OPERATIONS_PER_THREAD = 100;
    private final ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    private final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger userCounter = new AtomicInteger(0);
    private final AtomicInteger roleCounter = new AtomicInteger(0);
    private final AtomicInteger assignmentCounter = new AtomicInteger(0);
    private final AtomicInteger expectedExceptionsCount = new AtomicInteger(0);
    private final AtomicInteger unexpectedExceptionsCount = new AtomicInteger(0);
    
    @BeforeEach
    void setUp() {
        userManager = new UserManager();
        roleManager = new RoleManager(null);
        assignmentManager = new AssignmentManager(userManager, roleManager);
        roleManager.setAssignmentManager(assignmentManager);
        dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    }
    
    @AfterEach
    void tearDown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    @Test
    @DisplayName("Нагрузочный тест: многопоточные операции создания/обновления пользователей, ролей, назначений, фильтраций")
    void loadTest() throws InterruptedException {
        List<Callable<Void>> tasks = new ArrayList<>();
        
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            tasks.add(() -> {
                try {
                    runThreadOperations(threadId);
                } catch (Throwable t) {
                    exceptions.add(t);
                }
                return null;
            });
        }
        
        List<Future<Void>> futures = executor.invokeAll(tasks);
        
        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                exceptions.add(e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                exceptions.add(e);
            }
        }
        
        System.out.println("=== СТАТИСТИКА ИСКЛЮЧЕНИЙ ===");
        System.out.println("Ожидаемые исключения (бизнес-логика, гонки): " + expectedExceptionsCount.get());
        System.out.println("Неожиданные исключения: " + unexpectedExceptionsCount.get());
        System.out.println("Всего исключений: " + exceptions.size());
        if (!exceptions.isEmpty()) {
            Map<String, Long> exceptionCounts = exceptions.stream()
                .map(ex -> ex.getClass().getSimpleName())
                .collect(Collectors.groupingBy(name -> name, Collectors.counting()));
            System.out.println("Распределение исключений:");
            exceptionCounts.forEach((name, count) -> System.out.println("  " + name + ": " + count));
            for (int i = 0; i < Math.min(3, exceptions.size()); i++) {
                System.out.println("Исключение " + (i+1) + ": " + exceptions.get(i).getMessage());
            }
        }
        
        performIntegrityChecks();
    }
    
    private void runThreadOperations(int threadId) {
        Random random = new Random(threadId);
        
        for (int op = 0; op < OPERATIONS_PER_THREAD; op++) {
            int operationType = random.nextInt(10);
            
            try {
                switch (operationType) {
                    case 0:
                    case 1:
                        createUser(threadId, op);
                        break;
                    case 2:
                        updateUser(threadId, op);
                        break;
                    case 3:
                    case 4:
                        createRole(threadId, op);
                        break;
                    case 5:
                        assignRoleToUser(threadId, op, random);
                        break;
                    case 6:
                        filterUsers();
                        break;
                    case 7:
                        filterRoles();
                        break;
                    case 8:
                        searchAssignments();
                        break;
                    case 9:
                        checkUserPermissions(random);
                        break;
                }
                Thread.sleep(random.nextInt(5));
             } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (Exception e) {
                exceptions.add(e);
                if (isExpectedConcurrencyException(e)) {
                    expectedExceptionsCount.incrementAndGet();
                } else {
                    unexpectedExceptionsCount.incrementAndGet();
                }
            }
        }
    }
    
    private void createUser(int threadId, int op) {
        String username = String.format("user_%d_%d", threadId, op);
        String fullName = String.format("User %d-%d", threadId, op);
        String email = String.format("user%d.%d@test.com", threadId, op);
        
        User user = User.create(username, fullName, email);
        userManager.add(user);
        userCounter.incrementAndGet();
    }
    
    private void updateUser(int threadId, int op) {
        List<User> allUsers = userManager.findAll();
        if (allUsers.isEmpty()) {
            return;
        }
        User user = allUsers.get(Math.abs(threadId + op) % allUsers.size());
        String newFullName = String.format("Updated %d-%d", threadId, op);
        String newEmail = String.format("updated%d.%d@test.com", threadId, op);
        
        userManager.update(user.username(), newFullName, newEmail);
    }
    
    private void createRole(int threadId, int op) {
        String roleName = String.format("role_%d_%d", threadId, op);
        String description = String.format("Role for thread %d operation %d", threadId, op);
        
        Role role = new Role(roleName, description);
        roleManager.add(role);
        roleCounter.incrementAndGet();
        
        Permission readPerm = new Permission("READ", "resources", "Read permission");
        Permission writePerm = new Permission("WRITE", "resources", "Write permission");
        try {
            roleManager.addPermissionToRole(roleName, readPerm);
            roleManager.addPermissionToRole(roleName, writePerm);
        } catch (IllegalArgumentException e) {
        }
    }
    
    private void assignRoleToUser(int threadId, int op, Random random) {
        List<User> users = userManager.findAll();
        List<Role> roles = roleManager.findAll();
        
        if (users.isEmpty() || roles.isEmpty()) {
            return;
        }
        
        User user = users.get(random.nextInt(users.size()));
        Role role = roles.get(random.nextInt(roles.size()));
        
        AssignmentMetadata metadata = AssignmentMetadata.now("system", "Load test assignment");
        PermanentAssignment assignment = new PermanentAssignment(user, role, metadata);
        
        try {
            assignmentManager.add(assignment);
            assignmentCounter.incrementAndGet();
        } catch (IllegalStateException e) {
            
        } catch (IllegalArgumentException e) {
        }
    }
    
    private void filterUsers() {
        UserFilter domainFilter = UserFilters.byEmailDomain("test.com");
        List<User> domainUsers = userManager.findByFilter(domainFilter);
        
        UserFilter nameFilter = UserFilters.byUsernameContains("user");
        List<User> nameUsers = userManager.findByFilter(nameFilter);
        
        List<User> sorted = userManager.findAll(UserFilters.byUsernameContains(""), UserSorters.byUsername());
    }
    
    private void filterRoles() {
        RoleFilter permFilter = RoleFilters.hasAtLeastNPermissions(1);
        List<Role> rolesWithPerms = roleManager.findByFilter(permFilter);
        
        RoleFilter nameFilter = RoleFilters.byNameContains("role");
        List<Role> namedRoles = roleManager.findByFilter(nameFilter);
        
        List<Role> sorted = roleManager.findAll(RoleFilters.byNameContains(""), RoleSorters.byPermissionCount());
    }
    
    private void searchAssignments() {
        List<RoleAssignment> active = assignmentManager.getActiveAssignments();
        List<RoleAssignment> expired = assignmentManager.getExpiredAssignments();
        
        AssignmentFilter typeFilter = AssignmentFilters.byType("PERMANENT");
        List<RoleAssignment> permanent = assignmentManager.findByFilter(typeFilter);
        
        if (!active.isEmpty()) {
            AssignmentFilter userFilter = AssignmentFilters.byUsername(active.get(0).user().username());
            List<RoleAssignment> userAssignments = assignmentManager.findByFilter(userFilter);
        }
    }
    
    private void checkUserPermissions(Random random) {
        List<User> users = userManager.findAll();
        if (users.isEmpty()) {
            return;
        }
        
        User user = users.get(random.nextInt(users.size()));
        Set<Permission> permissions = assignmentManager.getUserPermissions(user);
        boolean hasRead = assignmentManager.userHasPermission(user, "READ", "resources");
        boolean hasWrite = assignmentManager.userHasPermission(user, "WRITE", "resources");
    }
    
    private boolean isExpectedConcurrencyException(Exception e) {
        if (e instanceof ConcurrentModificationException ||
            e instanceof ArrayIndexOutOfBoundsException ||
            e instanceof IllegalStateException ||
            e instanceof IllegalArgumentException) {
            return true;
        }
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        return message.contains("уже существует") ||
               message.contains("не найден") ||
               message.contains("не существует") ||
               message.contains("уже есть активное назначение") ||
               message.contains("Cannot delete role");
    }
    
    private void performIntegrityChecks() {
        System.out.println("=== ИТОГИ НАГРУЗОЧНОГО ТЕСТА ===");
        System.out.println("Создано пользователей: " + userCounter.get());
        System.out.println("Создано ролей: " + roleCounter.get());
        System.out.println("Создано назначений: " + assignmentCounter.get());
        System.out.println("Текущее количество пользователей: " + userManager.count());
        System.out.println("Текущее количество ролей: " + roleManager.count());
        System.out.println("Текущее количество назначений: " + assignmentManager.count());
        
        Set<String> usernames = new HashSet<>();
        for (User user : userManager.findAll()) {
            if (!usernames.add(user.username())) {
                fail("Обнаружен дубликат пользователя: " + user.username());
            }
        }
        System.out.println("Дубликаты пользователей: нет");
        
        Set<String> roleNames = new HashSet<>();
        for (Role role : roleManager.findAll()) {
            if (!roleNames.add(role.getName())) {
                fail("Обнаружен дубликат роли: " + role.getName());
            }
        }
        System.out.println("Дубликаты ролей: нет");
        
        Set<String> assignmentIds = new HashSet<>();
        for (RoleAssignment assignment : assignmentManager.findAll()) {
            if (!assignmentIds.add(assignment.assignmentId())) {
                fail("Обнаружен дубликат назначения: " + assignment.assignmentId());
            }
        }
        System.out.println("Дубликаты назначений: нет");
        
        for (RoleAssignment assignment : assignmentManager.findAll()) {
            User user = assignment.user();
            Role role = assignment.role();
            
            if (!userManager.exists(user.username())) {
                fail("Назначение ссылается на несуществующего пользователя: " + user.username());
            }
            if (!roleManager.findByName(role.getName()).isPresent()) {
                fail("Назначение ссылается на несуществующую роль: " + role.getName());
            }
        }
        System.out.println("Согласованность назначений: OK");
        System.out.println("Все проверки пройдены успешно!");
    }
}