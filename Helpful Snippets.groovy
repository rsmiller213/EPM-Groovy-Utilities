Date convertFiscalToCalendar (Date fiscalDate){

   List<String> monthsCalendarAndFiscalYearAreDifferent = ['Jul','Aug','Sep','Oct','Nov','Dec']
    Date calendarDate = new Date()
    if( monthsCalendarAndFiscalYearAreDifferent.contains(fiscalDate.format('MMM')) ) {
       calendarDate = new Date().parse('yyyy-MM-dd', ( fiscalDate.format('yyyy').toInteger() - 1 ) + fiscalDate.format('-MM-dd') )
    } else {
       calendarDate = fiscalDate
    }

   return calendarDate

}

Date convertCalendarToFiscal (Date calendarDate){

   List<String> monthsCalendarAndFiscalYearAreDifferent = ['Jul','Aug','Sep','Oct','Nov','Dec']
    Date fiscalDate = new Date()
    if( monthsCalendarAndFiscalYearAreDifferent.contains(calendarDate.format('MMM')) ) {
       fiscalDate = new Date().parse('yyyy-MM-dd', ( calendarDate.format('yyyy').toInteger() + 1 ) + calendarDate.format('-MM-dd') )
    } else {
       fiscalDate = calendarDate
    }

   return fiscalDate

}

