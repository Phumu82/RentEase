package com.rentease.app.presentation.info

import androidx.annotation.StringRes
import com.rentease.app.R

/** A title + body pair backed by string resources (so EN/ZU/VE all work). */
data class InfoItem(@StringRes val title: Int, @StringRes val body: Int)

data class PricingPlan(
    @StringRes val name: Int,
    @StringRes val price: Int,
    @StringRes val cadence: Int,
    @StringRes val description: Int,
    val features: List<Int>,
    @StringRes val cta: Int,
    val highlighted: Boolean,
    val forStudents: Boolean = false,
    val contactSales: Boolean = false
)

/** Static marketing copy, mirroring the web's marketing.ts. Content lives in strings.xml. */
object InfoContent {
    val aboutValues = listOf(
        InfoItem(R.string.about_value_safety_title, R.string.about_value_safety_body),
        InfoItem(R.string.about_value_transparency_title, R.string.about_value_transparency_body),
        InfoItem(R.string.about_value_fair_title, R.string.about_value_fair_body),
        InfoItem(R.string.about_value_students_title, R.string.about_value_students_body)
    )
    val aboutStory = listOf(R.string.about_story_1, R.string.about_story_2, R.string.about_story_3)

    val studentSteps = listOf(
        InfoItem(R.string.how_student_s1_title, R.string.how_student_s1_body),
        InfoItem(R.string.how_student_s2_title, R.string.how_student_s2_body),
        InfoItem(R.string.how_student_s3_title, R.string.how_student_s3_body),
        InfoItem(R.string.how_student_s4_title, R.string.how_student_s4_body)
    )
    val landlordSteps = listOf(
        InfoItem(R.string.how_landlord_s1_title, R.string.how_landlord_s1_body),
        InfoItem(R.string.how_landlord_s2_title, R.string.how_landlord_s2_body),
        InfoItem(R.string.how_landlord_s3_title, R.string.how_landlord_s3_body),
        InfoItem(R.string.how_landlord_s4_title, R.string.how_landlord_s4_body)
    )

    val services = listOf(
        InfoItem(R.string.services_s1_title, R.string.services_s1_body),
        InfoItem(R.string.services_s2_title, R.string.services_s2_body),
        InfoItem(R.string.services_s3_title, R.string.services_s3_body),
        InfoItem(R.string.services_s4_title, R.string.services_s4_body),
        InfoItem(R.string.services_s5_title, R.string.services_s5_body),
        InfoItem(R.string.services_s6_title, R.string.services_s6_body)
    )
    val servicesIncluded = listOf(
        R.string.services_included_1, R.string.services_included_2, R.string.services_included_3,
        R.string.services_included_4, R.string.services_included_5
    )

    val pricingPlans = listOf(
        PricingPlan(
            R.string.pricing_student_name, R.string.pricing_student_price, R.string.pricing_student_cadence,
            R.string.pricing_student_desc,
            listOf(
                R.string.pricing_student_f1, R.string.pricing_student_f2, R.string.pricing_student_f3,
                R.string.pricing_student_f4, R.string.pricing_student_f5, R.string.pricing_student_f6
            ),
            R.string.pricing_cta_student, highlighted = false, forStudents = true
        ),
        PricingPlan(
            R.string.pricing_growth_name, R.string.pricing_growth_price, R.string.pricing_growth_cadence,
            R.string.pricing_growth_desc,
            listOf(
                R.string.pricing_growth_f1, R.string.pricing_growth_f2, R.string.pricing_growth_f3,
                R.string.pricing_growth_f4, R.string.pricing_growth_f5, R.string.pricing_growth_f6
            ),
            R.string.pricing_cta_growth, highlighted = true
        ),
        PricingPlan(
            R.string.pricing_portfolio_name, R.string.pricing_portfolio_price, R.string.pricing_portfolio_cadence,
            R.string.pricing_portfolio_desc,
            listOf(
                R.string.pricing_portfolio_f1, R.string.pricing_portfolio_f2, R.string.pricing_portfolio_f3,
                R.string.pricing_portfolio_f4, R.string.pricing_portfolio_f5, R.string.pricing_portfolio_f6
            ),
            R.string.pricing_cta_portfolio, highlighted = false, contactSales = true
        )
    )

    val faqs = listOf(
        InfoItem(R.string.faq_q1, R.string.faq_a1),
        InfoItem(R.string.faq_q2, R.string.faq_a2),
        InfoItem(R.string.faq_q3, R.string.faq_a3),
        InfoItem(R.string.faq_q4, R.string.faq_a4),
        InfoItem(R.string.faq_q5, R.string.faq_a5),
        InfoItem(R.string.faq_q6, R.string.faq_a6),
        InfoItem(R.string.faq_q7, R.string.faq_a7)
    )

    val privacy = listOf(
        InfoItem(R.string.privacy_s1_title, R.string.privacy_s1_body),
        InfoItem(R.string.privacy_s2_title, R.string.privacy_s2_body),
        InfoItem(R.string.privacy_s3_title, R.string.privacy_s3_body),
        InfoItem(R.string.privacy_s4_title, R.string.privacy_s4_body)
    )
    val terms = listOf(
        InfoItem(R.string.terms_s1_title, R.string.terms_s1_body),
        InfoItem(R.string.terms_s2_title, R.string.terms_s2_body),
        InfoItem(R.string.terms_s3_title, R.string.terms_s3_body),
        InfoItem(R.string.terms_s4_title, R.string.terms_s4_body)
    )
}
