/* 
   Taken from the following page:
   
   http://coffeetime.solutions/automatic-page-numbering-in-atlassian-confluence/

   The trick is storing incremental numbers in the home page's content properties.
*/

import com.atlassian.confluence.pages.Page
import com.atlassian.confluence.core.ContentPropertyManager
import com.atlassian.sal.api.component.ComponentLocator
import com.atlassian.confluence.pages.PageManager
import com.atlassian.confluence.event.events.content.page.PageEvent
import org.jsoup.Jsoup

def contentPropertyManager = ComponentLocator.getComponent(ContentPropertyManager)
def pageManager = ComponentLocator.getComponent(PageManager)
def event = event as PageEvent
def page = event.getPage()
def pageLabels = page.getLabels()
def space = page.getSpace()
def homePage = space.getHomePage()
// Page created or updated inside of defined space
if (space.getKey() == "MySpaceKey"){
    // Check if the page has one of the defined labels (apn, something) and define the PREFIX
	def pageNrPrefix = null
    
      if (pageLabels.toString().contains("user-story")) {
        pageNrPrefix = "USERSTORY"
    }
    
	if (pageLabels.toString().contains("something")) {
        pageNrPrefix = "SOMETHING"
	}
    
	// If the defined label is set a prefix is defined and we can proceed
	if (pageNrPrefix) {
    	// Get page numbering of current page
    	def pageNr = null
    	def pageNrString = null
    	if (contentPropertyManager.getStringProperty(page, "pageNr")) {
        	pageNr = contentPropertyManager.getStringProperty(page, "pageNr").toInteger()
        	pageNrString = contentPropertyManager.getStringProperty(page, "pageNrString")
    	}
    	// Check if page count is already stored in homepage - if not set it
    	if (!contentPropertyManager.getStringProperty(homePage, "${pageNrPrefix}PageCount")) {
        	contentPropertyManager.setStringProperty(homePage, "${pageNrPrefix}PageCount", "0") 
    	}
    	// Calculate next page number if it is a new page
    	if (!pageNr) {
        	// Get next number and save it to homepage
        	def lastPageNr = contentPropertyManager.getStringProperty(homePage, "${pageNrPrefix}PageCount").toInteger()
        	def nextPageNr = lastPageNr + 1
        	pageNr = nextPageNr
        	pageNrString = "${pageNrPrefix}-${pageNr.toString()}"
        	contentPropertyManager.setStringProperty(homePage, "${pageNrPrefix}PageCount", nextPageNr.toString())
        	// Save Page Number to current page
        	contentPropertyManager.setStringProperty(page, "pageNr", pageNr.toString())
        	contentPropertyManager.setStringProperty(page, "pageNrString", pageNrString)
    	}
    	// Update Page Title if Page Number with prefix is not in page title
    	if (page.getTitle().indexOf(pageNrString.toString()) != 0) {
        	page.setTitle(pageNrString.toString() + " " + page.getTitle())
    	}
        
		// Update a placeholder text with userstory id
        log.debug "Inspecting page ${page.title}"
        def body = page.bodyContent.body
        def parsedBody = Jsoup.parse(body)
        def placeholderText = parsedBody.select('td:contains(PLACEHOLDERFORUSERSTORYID)')
        if (!placeholderText.empty) {
            log.debug "Found table header with placeholder text: ${placeholderText}"
            pageManager.saveNewVersion(page) { pageObject ->
                placeholderText.html(pageNrString.toString())
                pageObject.setBodyAsString(parsedBody.toString())
            }
        }        
        
	} 
}

