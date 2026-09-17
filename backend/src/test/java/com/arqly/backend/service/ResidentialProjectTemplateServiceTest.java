package com.arqly.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.arqly.backend.entity.ProjectPhaseTemplate;
import com.arqly.backend.entity.ProjectStageTemplate;
import com.arqly.backend.entity.ProjectTemplate;
import com.arqly.backend.entity.Tenant;
import com.arqly.backend.repository.ProjectPhaseTemplateRepository;
import com.arqly.backend.repository.ProjectStageTemplateRepository;
import com.arqly.backend.repository.ProjectTemplateRepository;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ResidentialProjectTemplateServiceTest {
    @Mock private ProjectTemplateRepository templates;
    @Mock private ProjectPhaseTemplateRepository phases;
    @Mock private ProjectStageTemplateRepository stages;

    @Test
    void createsResidentialTemplateOnlyForTheNewTenant() {
        Tenant tenant = new Tenant(); ReflectionTestUtils.setField(tenant, "id", UUID.randomUUID());
        when(templates.existsByTenantIdAndNameIgnoreCaseAndDeletedFalse(tenant.getId(), "Residencial")).thenReturn(false);
        when(templates.existsByTenantIdAndNameIgnoreCaseAndDeletedFalse(tenant.getId(), "Projeto Arquitetônico Residencial")).thenReturn(false);
        when(templates.save(any(ProjectTemplate.class))).thenAnswer(i -> i.getArgument(0));
        var savedStages = new ArrayList<ProjectStageTemplate>();
        when(phases.save(any(ProjectPhaseTemplate.class))).thenAnswer(i -> i.getArgument(0));
        when(stages.save(any(ProjectStageTemplate.class))).thenAnswer(i -> { savedStages.add(i.getArgument(0)); return i.getArgument(0); });
        new ResidentialProjectTemplateService(templates, phases, stages).ensureDefaultTemplate(tenant);
        var template = ArgumentCaptor.forClass(ProjectTemplate.class); verify(templates).save(template.capture());
        assertThat(template.getValue().getTenant()).isSameAs(tenant); assertThat(template.getValue().getName()).isEqualTo("Residencial");
        assertThat(savedStages).hasSize(8).allSatisfy(stage -> assertThat(stage.getTenant()).isSameAs(tenant));
    }
}
