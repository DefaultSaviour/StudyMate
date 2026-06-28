package uws.ac.uk.studymate.widget

import uws.ac.uk.studymate.R

class TallCalendarWidgetProvider : BaseCalendarWidgetProvider() {
    override val layoutId: Int = R.layout.widget_tall_calendar
    override val showBottomSections: Boolean = true
}
