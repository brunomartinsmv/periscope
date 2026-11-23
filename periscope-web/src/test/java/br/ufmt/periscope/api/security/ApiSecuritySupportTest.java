package br.ufmt.periscope.api.security;

import br.ufmt.periscope.api.ApiException;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.User;
import br.ufmt.periscope.model.UserLevel;
import java.util.List;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiSecuritySupportTest {

    private ApiSecuritySupport support;
    private User admin;
    private User owner;
    private User observer;
    private User outsider;
    private Project publicProject;

    @BeforeEach
    void setUp() {
        support = new ApiSecuritySupport();

        admin = new User();
        admin.setId(new ObjectId());
        admin.setUserLevel(UserLevel.ADMIN);

        owner = new User();
        owner.setId(new ObjectId());
        owner.setUserLevel(UserLevel.USER);

        observer = new User();
        observer.setId(new ObjectId());
        observer.setUserLevel(UserLevel.USER);

        outsider = new User();
        outsider.setId(new ObjectId());
        outsider.setUserLevel(UserLevel.USER);

        publicProject = new Project();
        publicProject.setId(new ObjectId());
        publicProject.setOwner(owner);
        publicProject.setObservers(List.of(observer));
        publicProject.setIsPublic(true);
    }

    @Test
    void allowsOwnerAndAdminToManageProject() {
        assertThat(support.canManage(publicProject, owner)).isTrue();
        assertThat(support.canManage(publicProject, admin)).isTrue();
        assertThatCode(() -> support.requireManager(publicProject, owner)).doesNotThrowAnyException();
        assertThatCode(() -> support.requireManager(publicProject, admin)).doesNotThrowAnyException();
    }

    @Test
    void blocksObserverAndPublicViewerFromManagingProject() {
        assertThat(support.canManage(publicProject, observer)).isFalse();
        assertThat(support.canManage(publicProject, outsider)).isFalse();

        assertThatThrownBy(() -> support.requireManager(publicProject, observer))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(403));

        assertThatThrownBy(() -> support.requireManager(publicProject, outsider))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(403));
    }
}
