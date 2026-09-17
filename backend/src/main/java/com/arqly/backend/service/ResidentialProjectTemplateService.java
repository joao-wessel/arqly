package com.arqly.backend.service;

import com.arqly.backend.entity.ProjectPhaseTemplate;
import com.arqly.backend.entity.ProjectStageTemplate;
import com.arqly.backend.entity.ProjectTemplate;
import com.arqly.backend.entity.ProgressCalculationMode;
import com.arqly.backend.entity.Tenant;
import com.arqly.backend.repository.ProjectPhaseTemplateRepository;
import com.arqly.backend.repository.ProjectStageTemplateRepository;
import com.arqly.backend.repository.ProjectTemplateRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ResidentialProjectTemplateService {
    private final ProjectTemplateRepository templates;
    private final ProjectPhaseTemplateRepository phases;
    private final ProjectStageTemplateRepository stages;

    public ResidentialProjectTemplateService(ProjectTemplateRepository templates, ProjectPhaseTemplateRepository phases, ProjectStageTemplateRepository stages) {
        this.templates = templates;
        this.phases = phases;
        this.stages = stages;
    }

    public void ensureDefaultTemplate(Tenant tenant) {
        if (templates.existsByTenantIdAndNameIgnoreCaseAndDeletedFalse(tenant.getId(), "Residencial")
                || templates.existsByTenantIdAndNameIgnoreCaseAndDeletedFalse(tenant.getId(), "Projeto Arquitetônico Residencial")) return;
        var template = new ProjectTemplate();
        template.setTenant(tenant); template.setName("Residencial");
        template.setDescription("Modelo inicial com fases e etapas padrão para projetos residenciais."); template.setActive(true);
        template = templates.save(template);
        for (PhaseDef definition : defaults()) {
            var phase = new ProjectPhaseTemplate();
            phase.setTenant(tenant); phase.setProjectTemplate(template); phase.setName(definition.name());
            phase.setDescription(definition.description()); phase.setOrder(definition.order()); phase.setColor(definition.color());
            phase.setIcon(definition.icon()); phase.setActive(true); phase = phases.save(phase);
            for (StageDef stageDef : definition.stages()) {
                var stage = new ProjectStageTemplate();
                stage.setTenant(tenant); stage.setProjectPhaseTemplate(phase); stage.setName(stageDef.name());
                stage.setDescription(stageDef.description()); stage.setOrder(stageDef.order()); stage.setWeightPercentage(stageDef.weight());
                stage.setProgressCalculationMode(ProgressCalculationMode.MANUAL); stage.setActive(true); stages.save(stage);
            }
        }
    }

    private List<PhaseDef> defaults() {
        return List.of(
                phase("Planejamento", "Organização inicial e entendimento do projeto.", 1, "#0f766e", "ClipboardList", stage("Levantamento", "Coleta de medidas, fotos e documentação inicial.", 1, "50"), stage("Programa de Necessidades", "Definição das necessidades do cliente.", 2, "50")),
                phase("Desenvolvimento", "Desenvolvimento técnico e conceitual.", 2, "#2563eb", "DraftingCompass", stage("Estudo Preliminar", "Primeiras soluções de projeto.", 1, "30"), stage("Anteprojeto", "Desenvolvimento das soluções aprovadas.", 2, "35"), stage("Projeto Executivo", "Detalhamento técnico para execução.", 3, "35")),
                phase("Aprovações", "Aprovações legais e validações externas.", 3, "#b45309", "BadgeCheck", stage("Projeto Legal", "Documentação para aprovação legal.", 1, "100")),
                phase("Execução", "Acompanhamento da execução contratada.", 4, "#7c3aed", "Hammer", stage("Acompanhamento da Obra", "Acompanhamento técnico da obra.", 1, "100")),
                phase("Encerramento", "Entrega final e encerramento técnico.", 5, "#475569", "PackageCheck", stage("Entrega", "Entrega final ao cliente.", 1, "100")));
    }
    private PhaseDef phase(String n,String d,int o,String c,String i,StageDef... s){return new PhaseDef(n,d,o,c,i,List.of(s));}
    private StageDef stage(String n,String d,int o,String w){return new StageDef(n,d,o,new BigDecimal(w));}
    private record PhaseDef(String name,String description,int order,String color,String icon,List<StageDef> stages){}
    private record StageDef(String name,String description,int order,BigDecimal weight){}
}
