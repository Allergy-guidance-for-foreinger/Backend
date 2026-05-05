package com.mealguide.mealguide_api.settings.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.settings.domain.AllergyGroup;
import com.mealguide.mealguide_api.settings.domain.AllergyOption;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AllergyJpaRepositoryTest {

    @Autowired
    private AllergyJpaRepository allergyJpaRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        entityManager.createNativeQuery("delete from allergy_translation").executeUpdate();
        entityManager.createNativeQuery("delete from allergy").executeUpdate();

        entityManager.createNativeQuery("""
                insert into allergy(code, name, allergy_group, display_order, created_at) values
                ('EGG', '난류', 'PRIMARY', 2, CURRENT_TIMESTAMP),
                ('MILK', '우유', 'PRIMARY', 1, CURRENT_TIMESTAMP),
                ('CELERY', '셀러리', 'ADDITIONAL', 101, CURRENT_TIMESTAMP)
                """).executeUpdate();

        entityManager.createNativeQuery("""
                insert into allergy_translation(id, allergy_code, lang_code, name, is_auto_translated, created_at, updated_at) values
                (1001, 'EGG', 'en', 'Egg', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                (1002, 'CELERY', 'en', 'Celery', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """).executeUpdate();
    }

    @Test
    void findAllergyOptionsByGroupReturnsPrimaryOnlySortedAndTranslated() {
        List<AllergyOption> options = allergyJpaRepository.findAllergyOptionsByGroup("en", AllergyGroup.PRIMARY);

        assertThat(options).extracting(AllergyOption::code).containsExactly("MILK", "EGG");
        assertThat(options).extracting(AllergyOption::name).containsExactly("우유", "Egg");
    }

    @Test
    void findAllergyOptionsByGroupReturnsAdditionalOnly() {
        List<AllergyOption> options = allergyJpaRepository.findAllergyOptionsByGroup("en", AllergyGroup.ADDITIONAL);

        assertThat(options).extracting(AllergyOption::code).containsExactly("CELERY");
        assertThat(options).extracting(AllergyOption::name).containsExactly("Celery");
    }
}
