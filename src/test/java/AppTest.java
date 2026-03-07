import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AppTest {

    private UserManager userManager;
    private RoleManager roleManager;
    private AssignmentManager assignmentManager;
    private DateTimeFormatter dateFormatter;

    private User testUser;
    private User anotherUser;
    private Role adminRole;
    private Role viewerRole;
    private Permission readPermission;
    private Permission writePermission;

    @BeforeEach
    void setUp() {
        // Создаем менеджеры с правильной инициализацией
        userManager = new UserManager();
        roleManager = new RoleManager(assignmentManager);
        assignmentManager = new AssignmentManager(userManager, roleManager);

        // Используем простой формат даты: yyyy-MM-dd HH:mm
        dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        testUser = User.create("john_doe", "John Doe", "john@example.com");
        anotherUser = User.create("jane_smith", "Jane Smith", "jane@example.com");

        readPermission = new Permission("READ", "users", "Can read users");
        writePermission = new Permission("WRITE", "users", "Can write users");

        adminRole = new Role("Administrator", "Admin role");
        adminRole.addPermission(readPermission);
        adminRole.addPermission(writePermission);

        viewerRole = new Role("Viewer", "Viewer role");
        viewerRole.addPermission(readPermission);
    }

    @Nested
    @DisplayName("UserManager Tests")
    class UserManagerTest {

        @Test
        @DisplayName("Should add user successfully")
        void testAddUser() {
            userManager.add(testUser);

            assertTrue(userManager.exists("john_doe"));
            assertEquals(1, userManager.count());
            assertEquals(testUser, userManager.findByUsername("john_doe").get());
        }

        @Test
        @DisplayName("Should throw exception when adding duplicate user")
        void testAddDuplicateUser() {
            userManager.add(testUser);

            assertThrows(IllegalArgumentException.class, () -> {
                userManager.add(testUser);
            });
        }

        @Test
        @DisplayName("Should remove user successfully")
        void testRemoveUser() {
            userManager.add(testUser);
            assertTrue(userManager.remove(testUser));
            assertFalse(userManager.exists("john_doe"));
        }

        @Test
        @DisplayName("Should find user by email")
        void testFindByEmail() {
            userManager.add(testUser);

            Optional<User> found = userManager.findByEmail("john@example.com");
            assertTrue(found.isPresent());
            assertEquals(testUser, found.get());

            assertTrue(userManager.findByEmail("nonexistent@example.com").isEmpty());
        }

        @Test
        @DisplayName("Should update user successfully")
        void testUpdateUser() {
            userManager.add(testUser);
            userManager.update("john_doe", "John Updated", "john.updated@example.com");

            User updated = userManager.findByUsername("john_doe").get();
            assertEquals("John Updated", updated.fullName());
            assertEquals("john.updated@example.com", updated.email());
        }

        @Test
        @DisplayName("Should throw exception when updating nonexistent user")
        void testUpdateNonexistentUser() {
            assertThrows(IllegalArgumentException.class, () -> {
                userManager.update("nonexistent", "New Name", "new@email.com");
            });
        }

        @Test
        @DisplayName("Should filter users correctly")
        void testFilterUsers() {
            userManager.add(testUser);
            userManager.add(anotherUser);

            UserFilter filter = UserFilters.byEmailDomain("example.com");
            List<User> filtered = userManager.findByFilter(filter);

            assertEquals(2, filtered.size());

            filter = UserFilters.byUsernameContains("jane");
            filtered = userManager.findByFilter(filter);

            assertEquals(1, filtered.size());
            assertEquals(anotherUser, filtered.get(0));
        }

        @Test
        @DisplayName("Should sort users correctly")
        void testSortUsers() {
            userManager.add(testUser);
            userManager.add(anotherUser);

            List<User> sorted = userManager.findAll(UserFilters.byUsernameContains(""),
                    UserSorters.byUsername());

            assertEquals("jane_smith", sorted.get(0).username());
            assertEquals("john_doe", sorted.get(1).username());
        }
    }

    @Nested
    @DisplayName("RoleManager Tests")
    class RoleManagerTest {

        @BeforeEach
        void setUp() {
            userManager.clear();
            roleManager.clear();
        }

        @Test
        @DisplayName("Should add role successfully")
        void testAddRole() {
            roleManager.add(adminRole);

            assertTrue(roleManager.exists("Administrator"));
            assertEquals(1, roleManager.count());
            assertEquals(adminRole, roleManager.findByName("Administrator").get());
        }

        @Test
        @DisplayName("Should throw exception when adding duplicate role name")
        void testAddDuplicateRole() {
            roleManager.add(adminRole);

            Role duplicateRole = new Role("Administrator", "Another admin");

            assertThrows(IllegalArgumentException.class, () -> {
                roleManager.add(duplicateRole);
            });
        }

        @Test
        @DisplayName("Should add permission to role")
        void testAddPermissionToRole() {
            roleManager.add(adminRole);
            Permission newPermission = new Permission("DELETE", "users", "Can delete");

            roleManager.addPermissionToRole("Administrator", newPermission);

            Role updated = roleManager.findByName("Administrator").get();
            assertTrue(updated.hasPermission(newPermission));
        }

        @Test
        @DisplayName("Should find roles with specific permission")
        void testFindRolesWithPermission() {
            roleManager.add(adminRole);
            roleManager.add(viewerRole);

            List<Role> roles = roleManager.findRolesWithPermission("READ", "users");
            assertEquals(2, roles.size());

            roles = roleManager.findRolesWithPermission("WRITE", "users");
            assertEquals(1, roles.size());
            assertEquals("Administrator", roles.get(0).getName());
        }

        @Test
        @DisplayName("Should filter roles correctly")
        void testFilterRoles() {
            roleManager.add(adminRole);
            roleManager.add(viewerRole);

            RoleFilter filter = RoleFilters.hasPermission(readPermission);
            List<Role> filtered = roleManager.findByFilter(filter);

            assertEquals(2, filtered.size());

            filter = RoleFilters.hasAtLeastNPermissions(2);
            filtered = roleManager.findByFilter(filter);

            assertEquals(1, filtered.size());
            assertEquals(adminRole, filtered.get(0));
        }

        @Test
        @DisplayName("Should sort roles by permission count")
        void testSortRolesByPermissionCount() {
            roleManager.add(adminRole);
            roleManager.add(viewerRole);

            List<Role> sorted = roleManager.findAll(RoleFilters.byNameContains(""),
                    RoleSorters.byPermissionCount());

            assertEquals(viewerRole, sorted.get(0));
            assertEquals(adminRole, sorted.get(1));
        }
    }

    @Nested
    @DisplayName("AssignmentManager Tests")
    class AssignmentManagerTest {

        private PermanentAssignment permanentAssignment;
        private TemporaryAssignment temporaryAssignment;
        private AssignmentMetadata metadata;

        @BeforeEach
        void setUp() {
            userManager.add(testUser);
            userManager.add(anotherUser);
            roleManager.add(adminRole);
            roleManager.add(viewerRole);

            metadata = AssignmentMetadata.now("admin", "Test assignment");
            permanentAssignment = new PermanentAssignment(testUser, adminRole, metadata);

            String futureDate = LocalDateTime.now().plusDays(10).format(dateFormatter);
            temporaryAssignment = new TemporaryAssignment(anotherUser, viewerRole, metadata, futureDate, false);
        }

        @Test
        @DisplayName("Should add assignment successfully")
        void testAddAssignment() {
            assignmentManager.add(permanentAssignment);

            assertEquals(1, assignmentManager.count());
            assertTrue(assignmentManager.findById(permanentAssignment.assignmentId()).isPresent());
        }

        @Test
        @DisplayName("Should throw exception when adding duplicate assignment")
        void testAddDuplicateAssignment() {
            assignmentManager.add(permanentAssignment);

            assertThrows(IllegalArgumentException.class, () -> {
                assignmentManager.add(permanentAssignment);
            });
        }

        @Test
        @DisplayName("Should throw exception when adding assignment for nonexistent user")
        void testAddAssignmentForNonexistentUser() {
            User nonexistent = User.create("nonexistent", "No Name", "no@email.com");
            AssignmentMetadata meta = AssignmentMetadata.now("admin", "Test");
            PermanentAssignment badAssignment = new PermanentAssignment(nonexistent, adminRole, meta);

            assertThrows(IllegalArgumentException.class, () -> {
                assignmentManager.add(badAssignment);
            });
        }

        @Test
        @DisplayName("Should throw exception when adding duplicate active assignment")
        void testAddDuplicateActiveAssignment() {
            assignmentManager.add(permanentAssignment);

            AssignmentMetadata newMetadata = AssignmentMetadata.now("admin", "Another assignment");
            PermanentAssignment duplicate = new PermanentAssignment(testUser, adminRole, newMetadata);

            assertThrows(IllegalStateException.class, () -> {
                assignmentManager.add(duplicate);
            });
        }

        @Test
        @DisplayName("Should find assignments by user")
        void testFindByUser() {
            assignmentManager.add(permanentAssignment);
            assignmentManager.add(temporaryAssignment);

            List<RoleAssignment> userAssignments = assignmentManager.findByUser(testUser);
            assertEquals(1, userAssignments.size());
            assertEquals(permanentAssignment, userAssignments.get(0));

            userAssignments = assignmentManager.findByUser(anotherUser);
            assertEquals(1, userAssignments.size());
            assertEquals(temporaryAssignment, userAssignments.get(0));
        }

        @Test
        @DisplayName("Should find assignments by role")
        void testFindByRole() {
            assignmentManager.add(permanentAssignment);
            assignmentManager.add(temporaryAssignment);

            List<RoleAssignment> roleAssignments = assignmentManager.findByRole(adminRole);
            assertEquals(1, roleAssignments.size());
            assertEquals(permanentAssignment, roleAssignments.get(0));
        }

        @Test
        @DisplayName("Should get active assignments correctly")
        void testGetActiveAssignments() {
            assignmentManager.add(permanentAssignment);
            assignmentManager.add(temporaryAssignment);
            assertEquals(2, assignmentManager.getActiveAssignments().size());
            String pastDate = LocalDateTime.now().minusDays(10).format(dateFormatter);
            User thirdUser = User.create("bob_wilson", "Bob Wilson", "bob@example.com");
            userManager.add(thirdUser);
            TemporaryAssignment expiredAssignment = new TemporaryAssignment(
                    thirdUser, viewerRole, metadata, pastDate, false);
            assignmentManager.add(expiredAssignment);
            assertEquals(2, assignmentManager.getActiveAssignments().size());
        }

        @Test
        @DisplayName("Should check if user has role correctly")
        void testUserHasRole() {
            assignmentManager.add(permanentAssignment);

            assertTrue(assignmentManager.userHasRole(testUser, adminRole));
            assertFalse(assignmentManager.userHasRole(testUser, viewerRole));
            assertFalse(assignmentManager.userHasRole(anotherUser, adminRole));
        }

        @Test
        @DisplayName("Should get user permissions correctly")
        void testGetUserPermissions() {
            assignmentManager.add(permanentAssignment);

            Set<Permission> permissions = assignmentManager.getUserPermissions(testUser);

            assertEquals(2, permissions.size());
            assertTrue(permissions.contains(readPermission));
            assertTrue(permissions.contains(writePermission));
        }

        @Test
        @DisplayName("Should check if user has permission correctly")
        void testUserHasPermission() {
            assignmentManager.add(permanentAssignment);

            assertTrue(assignmentManager.userHasPermission(testUser, "READ", "users"));
            assertTrue(assignmentManager.userHasPermission(testUser, "WRITE", "users"));
            assertFalse(assignmentManager.userHasPermission(testUser, "DELETE", "users"));
        }

        @Test
        @DisplayName("Should revoke permanent assignment")
        void testRevokePermanentAssignment() {
            assignmentManager.add(permanentAssignment);

            assertTrue(permanentAssignment.isActive());

            assignmentManager.revokeAssignment(permanentAssignment.assignmentId());

            assertFalse(permanentAssignment.isActive());
            assertTrue(permanentAssignment.isRevoked());
        }

        @Test
        @DisplayName("Should remove temporary assignment when revoked")
        void testRevokeTemporaryAssignment() {
            assignmentManager.add(temporaryAssignment);

            String id = temporaryAssignment.assignmentId();
            assertTrue(assignmentManager.findById(id).isPresent());

            assignmentManager.revokeAssignment(id);

            assertTrue(assignmentManager.findById(id).isEmpty());
        }

        @Test
        @DisplayName("Should extend temporary assignment")
        void testExtendTemporaryAssignment() {
            assignmentManager.add(temporaryAssignment);

            String newDate = LocalDateTime.now().plusDays(20).format(dateFormatter);
            assignmentManager.extendTemporaryAssignment(temporaryAssignment.assignmentId(), newDate);

            assertEquals(newDate, ((TemporaryAssignment) temporaryAssignment).getExpiresAt());
        }

        @Test
        @DisplayName("Should throw exception when extending non-temporary assignment")
        void testExtendNonTemporaryAssignment() {
            assignmentManager.add(permanentAssignment);

            String newDate = LocalDateTime.now().plusDays(20).format(dateFormatter);

            assertThrows(IllegalArgumentException.class, () -> {
                assignmentManager.extendTemporaryAssignment(permanentAssignment.assignmentId(), newDate);
            });
        }

        @Test
        @DisplayName("Should filter assignments correctly")
        void testFilterAssignments() {
            assignmentManager.add(permanentAssignment);
            assignmentManager.add(temporaryAssignment);

            AssignmentFilter filter = AssignmentFilters.byType("PERMANENT");
            List<RoleAssignment> filtered = assignmentManager.findByFilter(filter);

            assertEquals(1, filtered.size());
            assertEquals(permanentAssignment, filtered.get(0));

            filter = AssignmentFilters.byUsername("john_doe");
            filtered = assignmentManager.findByFilter(filter);

            assertEquals(1, filtered.size());
            assertEquals(permanentAssignment, filtered.get(0));
        }

        @Test
        @DisplayName("Should sort assignments correctly")
        void testSortAssignments() {
            assignmentManager.add(permanentAssignment);
            assignmentManager.add(temporaryAssignment);
            List<RoleAssignment> sorted = assignmentManager.findAll(
                    AssignmentFilters.byType("PERMANENT"),
                    AssignmentSorters.byUsername()
            );
            AssignmentFilter allFilter = assignment -> true;
            List<RoleAssignment> sorted2 = assignmentManager.findAll(
                    allFilter,
                    AssignmentSorters.byUsername()
            );
            assertEquals(anotherUser.username(), sorted2.get(0).user().username());
            assertEquals(testUser.username(), sorted2.get(1).user().username());
        }

        @Test
        @DisplayName("Should get assignments expiring before date")
        void testGetAssignmentsExpiringBefore() {
            String futureDate = LocalDateTime.now().plusDays(5).format(dateFormatter);
            String furtherDate = LocalDateTime.now().plusDays(15).format(dateFormatter);

            TemporaryAssignment soonExpiring = new TemporaryAssignment(
                    testUser, viewerRole, metadata, futureDate, false);
            assignmentManager.add(soonExpiring);
            assignmentManager.add(temporaryAssignment);

            String checkDate = LocalDateTime.now().plusDays(10).format(dateFormatter);
            List<RoleAssignment> expiring = assignmentManager.getAssignmentsExpiringBefore(checkDate);

            assertEquals(1, expiring.size());
            assertEquals(soonExpiring, expiring.get(0));
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Complete RBAC flow test")
        void testCompleteRBACFlow() {
            userManager.add(testUser);
            userManager.add(anotherUser);
            roleManager.add(adminRole);
            roleManager.add(viewerRole);

            Permission deletePermission = new Permission("DELETE", "users", "Can delete users");
            roleManager.addPermissionToRole("Administrator", deletePermission);

            AssignmentMetadata adminMeta = AssignmentMetadata.now("system", "Initial admin assignment");
            AssignmentMetadata viewerMeta = AssignmentMetadata.now("system", "Initial viewer assignment");

            PermanentAssignment adminAssignment = new PermanentAssignment(testUser, adminRole, adminMeta);
            String tempExpiry = LocalDateTime.now().plusDays(30).format(dateFormatter);
            TemporaryAssignment viewerAssignment = new TemporaryAssignment(
                    anotherUser, viewerRole, viewerMeta, tempExpiry, true);

            assignmentManager.add(adminAssignment);
            assignmentManager.add(viewerAssignment);
            assertTrue(assignmentManager.userHasPermission(testUser, "READ", "users"));
            assertTrue(assignmentManager.userHasPermission(testUser, "WRITE", "users"));
            assertTrue(assignmentManager.userHasPermission(testUser, "DELETE", "users"));
            assertTrue(assignmentManager.userHasPermission(anotherUser, "READ", "users"));
            assertFalse(assignmentManager.userHasPermission(anotherUser, "WRITE", "users"));
            List<RoleAssignment> active = assignmentManager.getActiveAssignments();
            assertEquals(2, active.size());
            assignmentManager.revokeAssignment(adminAssignment.assignmentId());
            assertFalse(assignmentManager.userHasPermission(testUser, "READ", "users"));
            assertTrue(assignmentManager.userHasPermission(anotherUser, "READ", "users"));
            String newExpiry = LocalDateTime.now().plusDays(60).format(dateFormatter);
            assignmentManager.extendTemporaryAssignment(viewerAssignment.assignmentId(), newExpiry);
            assertEquals(newExpiry, ((TemporaryAssignment) viewerAssignment).getExpiresAt());
        }
    }
}