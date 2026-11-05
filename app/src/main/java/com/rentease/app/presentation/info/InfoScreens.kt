package com.rentease.app.presentation.info

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rentease.app.R
import com.rentease.app.presentation.common.HeroBanner
import com.rentease.app.domain.model.ContactForm

/** Shared frame for every public info page: back arrow, title, scrolling padded content. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InfoScaffold(
    @StringRes title: Int,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_button))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            HeroBanner(title = stringResource(title), height = 140.dp)
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) { content() }
        }
    }
}

@Composable
private fun Headline(@StringRes title: Int, @StringRes intro: Int? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.headlineSmall)
        if (intro != null) {
            Text(
                stringResource(intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoCard(item: InfoItem, number: Int? = null) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (number != null) {
                val label = number.toString()
                Text(label, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(item.title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(item.body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CheckRow(@StringRes text: Int) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(text), style = MaterialTheme.typography.bodyMedium)
    }
}

// ---------------------------------------------------------------- hub

private data class HubEntry(@StringRes val label: Int, val route: InfoPage)

enum class InfoPage { UNIVERSITIES, ABOUT, HOW_IT_WORKS, SERVICES, PRICING, FAQ, CONTACT, PRIVACY, TERMS }

@Composable
fun InfoHubScreen(onBack: () -> Unit, onOpen: (InfoPage) -> Unit) {
    val entries = listOf(
        HubEntry(R.string.pub_universities, InfoPage.UNIVERSITIES),
        HubEntry(R.string.pub_about_us, InfoPage.ABOUT),
        HubEntry(R.string.pub_how_it_works, InfoPage.HOW_IT_WORKS),
        HubEntry(R.string.pub_services, InfoPage.SERVICES),
        HubEntry(R.string.pub_pricing, InfoPage.PRICING),
        HubEntry(R.string.pub_faq, InfoPage.FAQ),
        HubEntry(R.string.pub_contact, InfoPage.CONTACT),
        HubEntry(R.string.pub_privacy, InfoPage.PRIVACY),
        HubEntry(R.string.pub_terms, InfoPage.TERMS)
    )
    InfoScaffold(R.string.pub_more_about, onBack) {
        entries.forEach { entry ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onOpen(entry.route) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(entry.label), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- pages

@Composable
fun AboutScreen(onBack: () -> Unit) {
    InfoScaffold(R.string.pub_about_us, onBack) {
        Headline(R.string.about_title, R.string.about_intro)
        Text(stringResource(R.string.about_story_title), style = MaterialTheme.typography.titleLarge)
        InfoContent.aboutStory.forEach { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium) }
        Text(stringResource(R.string.about_values_title), style = MaterialTheme.typography.titleLarge)
        InfoContent.aboutValues.forEach { InfoCard(it) }
    }
}

@Composable
fun HowItWorksScreen(onBack: () -> Unit, onCreateStudent: () -> Unit, onCreateLandlord: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    InfoScaffold(R.string.pub_how_it_works, onBack) {
        Headline(R.string.how_title, R.string.how_intro)
        SecondaryTabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.how_student_tab)) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.how_landlord_tab)) })
        }
        if (tab == 0) {
            Text(stringResource(R.string.how_student_heading), style = MaterialTheme.typography.titleLarge)
            InfoContent.studentSteps.forEachIndexed { i, s -> InfoCard(s, number = i + 1) }
            Button(onClick = onCreateStudent, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.how_cta_student))
            }
        } else {
            Text(stringResource(R.string.how_landlord_heading), style = MaterialTheme.typography.titleLarge)
            InfoContent.landlordSteps.forEachIndexed { i, s -> InfoCard(s, number = i + 1) }
            Button(onClick = onCreateLandlord, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.how_cta_landlord))
            }
        }
    }
}

@Composable
fun ServicesScreen(onBack: () -> Unit, onContact: () -> Unit, onPricing: () -> Unit) {
    InfoScaffold(R.string.pub_services, onBack) {
        Headline(R.string.services_title, R.string.services_intro)
        InfoContent.services.forEach { InfoCard(it) }
        Text(stringResource(R.string.services_included_title), style = MaterialTheme.typography.titleLarge)
        InfoContent.servicesIncluded.forEach { CheckRow(it) }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.services_cta_title), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Text(stringResource(R.string.services_cta_body), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Button(onClick = onContact, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.pub_contact)) }
                OutlinedButton(onClick = onPricing, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.pub_pricing)) }
            }
        }
    }
}

@Composable
fun PricingScreen(onBack: () -> Unit, onCreateStudent: () -> Unit, onCreateLandlord: () -> Unit, onContact: () -> Unit) {
    InfoScaffold(R.string.pub_pricing, onBack) {
        Headline(R.string.pricing_title, R.string.pricing_intro)
        InfoContent.pricingPlans.forEach { plan ->
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(if (plan.highlighted) 6.dp else 1.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (plan.highlighted) {
                        Text(stringResource(R.string.pricing_popular), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(stringResource(plan.name), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(plan.description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(stringResource(plan.price), style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(plan.cadence), style = MaterialTheme.typography.bodyMedium)
                    }
                    plan.features.forEach { CheckRow(it) }
                    Spacer(Modifier.height(4.dp))
                    val action = when {
                        plan.contactSales -> onContact
                        plan.forStudents -> onCreateStudent
                        else -> onCreateLandlord
                    }
                    if (plan.highlighted) {
                        Button(onClick = action, modifier = Modifier.fillMaxWidth()) { Text(stringResource(plan.cta)) }
                    } else {
                        OutlinedButton(onClick = action, modifier = Modifier.fillMaxWidth()) { Text(stringResource(plan.cta)) }
                    }
                }
            }
        }
    }
}

@Composable
fun FaqScreen(onBack: () -> Unit, onContact: () -> Unit) {
    var open by rememberSaveable { mutableIntStateOf(-1) }
    InfoScaffold(R.string.pub_faq, onBack) {
        Headline(R.string.faq_title, R.string.faq_intro)
        InfoContent.faqs.forEachIndexed { index, faq ->
            val expanded = open == index
            Card(modifier = Modifier.fillMaxWidth().clickable { open = if (expanded) -1 else index }) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(faq.title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Icon(
                            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null
                        )
                    }
                    if (expanded) {
                        Text(stringResource(faq.body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Text(stringResource(R.string.faq_not_found), style = MaterialTheme.typography.titleMedium)
        Button(onClick = onContact, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.faq_contact_support)) }
    }
}

@Composable
fun LegalScreen(@StringRes title: Int, sections: List<InfoItem>, onBack: () -> Unit) {
    InfoScaffold(title, onBack) {
        Text(stringResource(R.string.pub_last_updated), style = MaterialTheme.typography.bodySmall)
        sections.forEach { s ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(s.title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(s.body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) = LegalScreen(R.string.pub_privacy, InfoContent.privacy, onBack)

@Composable
fun TermsScreen(onBack: () -> Unit) = LegalScreen(R.string.pub_terms, InfoContent.terms, onBack)

// ---------------------------------------------------------------- contact

@Composable
fun ContactScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var subject by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var errors by remember { mutableStateOf(emptySet<ContactForm.Field>()) }
    var noEmailApp by rememberSaveable { mutableStateOf(false) }

    val phone = stringResource(R.string.contact_phone)
    val address = stringResource(R.string.contact_address)
    val supportEmail = stringResource(R.string.contact_email)

    fun launch(intent: Intent): Boolean = try {
        context.startActivity(intent); true
    } catch (e: ActivityNotFoundException) { false }

    InfoScaffold(R.string.pub_contact, onBack) {
        Headline(R.string.contact_title, R.string.contact_intro)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.contact_head_office), style = MaterialTheme.typography.titleMedium)
                ContactLine(Icons.Filled.LocationOn, address, onClick = null)
                ContactLine(Icons.Filled.Phone, phone) {
                    launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.filter { it.isDigit() || it == '+' })))
                }
                ContactLine(Icons.Filled.Email, supportEmail) {
                    if (!launch(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$supportEmail")))) noEmailApp = true
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.contact_hours_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.contact_hours_body), style = MaterialTheme.typography.bodyMedium)
            }
        }

        ContactField(R.string.contact_form_name, name, ContactForm.NAME_MAX, ContactForm.Field.NAME in errors, R.string.contact_error_name) { name = it }
        ContactField(R.string.contact_form_email, email, ContactForm.EMAIL_MAX, ContactForm.Field.EMAIL in errors, R.string.contact_error_email) { email = it }
        ContactField(R.string.contact_form_subject, subject, ContactForm.SUBJECT_MAX, ContactForm.Field.SUBJECT in errors, R.string.contact_error_subject) { subject = it }
        ContactField(R.string.contact_form_message, message, ContactForm.MESSAGE_MAX, ContactForm.Field.MESSAGE in errors, R.string.contact_error_message, minLines = 5) { message = it }

        Text(stringResource(R.string.contact_open_email_hint), style = MaterialTheme.typography.bodySmall)
        if (noEmailApp) {
            Text(stringResource(R.string.contact_no_email_app), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = {
                errors = ContactForm.validate(name, email, subject, message)
                if (errors.isEmpty()) {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(ContactForm.SUPPORT_EMAIL))
                        putExtra(Intent.EXTRA_SUBJECT, subject.trim())
                        putExtra(Intent.EXTRA_TEXT, ContactForm.emailBody(name, email, message))
                    }
                    noEmailApp = !launch(intent)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.contact_form_send)) }
    }
}

@Composable
private fun ContactLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: (() -> Unit)?) {
    val base = Modifier.fillMaxWidth()
    Row(
        modifier = if (onClick != null) base.clickable(onClick = onClick) else base,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ContactField(
    @StringRes label: Int,
    value: String,
    maxLength: Int,
    isError: Boolean,
    @StringRes errorText: Int,
    minLines: Int = 1,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= maxLength) onChange(it) },
        label = { Text(stringResource(label)) },
        isError = isError,
        supportingText = if (isError) ({ Text(stringResource(errorText)) }) else null,
        minLines = minLines,
        modifier = Modifier.fillMaxWidth()
    )
}
