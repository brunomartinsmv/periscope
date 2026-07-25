package br.ufmt.periscope.api.dto;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.User;
import br.ufmt.periscope.model.UserLevel;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class DtoMappingTest {

    @Test
    void userDtoNeverExposesPassword() {
        User user = new User();
        user.setId(new ObjectId());
        user.setUsername("admin");
        user.setPassword("secret-or-hash");
        user.setFirstname("Admin");
        user.setLastname("User");
        user.setEmail("admin@example.com");
        user.setUserLevel(UserLevel.ADMIN);

        UserDTO dto = UserDTO.from(user);

        assertThat(dto.id()).isEqualTo(user.getId().toString());
        assertThat(dto.username()).isEqualTo("admin");
        assertThat(dto.firstname()).isEqualTo("Admin");
        assertThat(dto.userLevel()).isEqualTo("ADMIN");
        assertThat(dto.toString()).doesNotContain("secret");
        assertThat(dto.getClass().getRecordComponents())
                .extracting(rc -> rc.getName())
                .doesNotContain("password");
    }

    @Test
    void projectDtoMapsOwnerAndCounts() {
        User owner = new User();
        owner.setId(new ObjectId());
        owner.setFirstname("Ada");
        owner.setLastname("Lovelace");

        Project project = new Project();
        project.setId(new ObjectId());
        project.setTitle("Solar");
        project.setDescription("desc");
        project.setIsPublic(true);
        project.setCreatedAt(new Date());
        project.setUpdateAt(new Date());
        project.setOwner(owner);

        ProjectDTO dto = ProjectDTO.from(project, 42);

        assertThat(dto.id()).isEqualTo(project.getId().toString());
        assertThat(dto.title()).isEqualTo("Solar");
        assertThat(dto.ownerId()).isEqualTo(owner.getId().toString());
        assertThat(dto.ownerName()).contains("Ada");
        assertThat(dto.patentCount()).isEqualTo(42);
        assertThat(dto.isPublic()).isTrue();
    }

    @Test
    void patentDtoFromWithExplicitProjectIdDoesNotNeedProjectReference() {
        br.ufmt.periscope.model.Patent patent = new br.ufmt.periscope.model.Patent();
        patent.setId(new ObjectId());
        patent.setTitleSelect("Solar Tracking");
        patent.setPublicationNumber("DE1");
        // deliberately leave project null — list path must not dereference it
        patent.setProject(null);

        String projectId = new ObjectId().toString();
        PatentDTO dto = PatentDTO.from(patent, projectId);

        assertThat(dto.projectId()).isEqualTo(projectId);
        assertThat(dto.title()).isEqualTo("SOLAR TRACKING");
        assertThat(dto.id()).isEqualTo(patent.getId().toString());
    }
}
