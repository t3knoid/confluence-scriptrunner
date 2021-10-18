// Note: Create a new page using pre-defined templates
// Events: FormSubmitEvent

import com.atlassian.confluence.core.DefaultSaveContext
import com.atlassian.confluence.pages.Page
import com.atlassian.confluence.pages.PageManager
import com.atlassian.confluence.spaces.SpaceManager
import com.atlassian.confluence.pages.templates.PageTemplateManager
import com.atlassian.sal.api.component.ComponentLocator
import com.atlassian.xwork.FileUploadUtils
import groovy.json.JsonSlurper
import org.jsoup.Jsoup

String event = ((HashMap<String, String>) binding.variables).get("event").structuredValue
Map<String, String> inputtedValues = new JsonSlurper().parseText(event)
List<FileUploadUtils.UploadedFile> uploadedFiles = ((HashMap<String, String>) binding.variables).get("event").uploadedFiles

def spaceKey = "PRJ"
def parentPageTitle = "New Page"
def pageManager = ComponentLocator.getComponent(PageManager)
def spaceManager = ComponentLocator.getComponent(SpaceManager)
def templateManager = ComponentLocator.getComponent(PageTemplateManager)
def template = templateManager.getPageTemplate(118325297) 
   
// Define content from entered form values
// Ensure these fields are set to 
def projectName = getValue(inputtedValues , "projectName")

def pageTitle = projectName // Define title
def pageContent = template.getContent()  // Use template content

// construct a new page
Page targetPage = constructNewPage(spaceManager, spaceKey, pageTitle, pageContent)
validateSpace(spaceKey, spaceManager)
validateParentPage(spaceKey, parentPageTitle, pageManager)
try {
    setPageAncestryAndSave(parentPageTitle, targetPage, spaceKey, pageManager)
}
catch (anyException) {
    log.info("Failed to create page for new user", anyException)
}

/*
    Need to modify the page content with specific
    Information. We are going to use Jsoup.
*/  

try 
{
    // This example looks for a specific JQL from the Jira macro and modifes it
    def rootPage = pageManager.getPage(spaceKey, pageTitle) //You can change this line to point to the right parent page.

    log.debug "Inspecting page ${page.title}"
    def body = page.bodyContent.body
    def parsedBody = Jsoup.parse(body)
    def newJql = "project = PRJ AND resolution = Unresolved"
    
    // Look for all jqlQuery values
    def jqlToChange = parsedBody.select('[ac:macro-id="6f244406-354f-4354-adab-58fac98db746"]')
    if (!jqlToChange.empty) {
        log.debug "Found Jira sql: ${jqlToChange}"
        pageManager.saveNewVersion(page) { pageObject ->
            jqlToChange.text("Changed text")
            pageObject.setBodyAsString(parsedBody.toString())
        }
    }
}
catch (anyException) {
    log.info("Failed to change text", anyException)
}

// Methods start here

private static String getValue(Map<String, String[]> data, String key) {
    if (!data.get(key) || data.get(key)[0].isEmpty()) {
        throw new IllegalArgumentException("A \"" + key + "\" was not provided.")
    } else if (hasMultipleUniqueEntries(data.get(key) as List<String>)) {
        throw new IllegalArgumentException("multiple \" " + key + "\"'s were provided, please enter a single \"" + key + "\"")
    }
    data.get(key)[0]
}

private static boolean hasMultipleUniqueEntries(List<String> entries) {
    Set uniqueEntries = [] as Set
    uniqueEntries.addAll(entries)
    uniqueEntries.size() != 1
}

//private static Page constructNewPage(SpaceManager spaceManager, String spaceKey, String pageTitle, Page parentPage, String pageContent) {
private static Page constructNewPage(SpaceManager spaceManager, String spaceKey, String pageTitle, String pageContent) {
    def targetPage = new Page(
        space: spaceManager.getSpace(spaceKey),
        title: pageTitle,
        bodyAsString: pageContent,
    )
    targetPage
}

private static validateSpace(String spaceKey, SpaceManager spaceManager) {
    def space = spaceManager.getSpace(spaceKey)
    if (space == null) {
        throw new IllegalArgumentException("invalid space key")
    }
}

private static validateParentPage(String spaceKey, String parentPageTitle, PageManager pageManager) {
    def parentPage = pageManager.getPage(spaceKey, parentPageTitle)
    if (parentPage == null) {
        throw new IllegalArgumentException("invalid parentPageTitle. " + parentPageTitle + " is not found in " + spaceKey)
    }
}

private static setPageAncestryAndSave(String parentPageTitle, Page targetPage, String spaceKey, PageManager pageManager) {
    Page parentPage = pageManager.getPage(spaceKey, parentPageTitle)
    parentPage.addChild(targetPage)
    targetPage.setParentPage(parentPage)

    pageManager.saveContentEntity(parentPage, DefaultSaveContext.DEFAULT)
    pageManager.saveContentEntity(targetPage, DefaultSaveContext.DEFAULT)
}
