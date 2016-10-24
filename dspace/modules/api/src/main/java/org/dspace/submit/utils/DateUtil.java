/*
* @(#)DateUtil.java 1.0 2016/10/23
*
*        Author: Debra Fagan
*   Orgnization: Dryad Digital Repository
*          Path: dryad-repo/dspace/modules/api/src/main/java/org/dspace/submit/utils/DateUtil.java
*       Created: 2016-10-23
* Last Modified: 2016-10-23
*	    Purpose: This program creates Dryad's weekly curation reports.
*/

package org.dspace.submit.utils;

import java.util.Calendar;

public class DateUtil {

		private long StartOfWeek;
		private long StartOfMonth;
		private long EndOfWeek;
		private long EndOfMonth;
		
		public DateUtil(){
				Calendar cal_week = Calendar.getInstance();
				cal_week.set(Calendar.HOUR_OF_DAY, 0); 
				cal_week.clear(Calendar.MINUTE);
				cal_week.clear(Calendar.SECOND);
				cal_week.clear(Calendar.MILLISECOND);
				
				// get start of this week in milliseconds
				cal_week.set(Calendar.DAY_OF_WEEK, cal_week.getFirstDayOfWeek());
				StartOfWeek = cal_week.getTimeInMillis();
				
				// start of the next week
				cal_week.add(Calendar.WEEK_OF_YEAR, 1);
				EndOfWeek = cal_week.getTimeInMillis();
				
				Calendar cal_month = Calendar.getInstance();
				cal_month.set(Calendar.HOUR_OF_DAY, 0); 
				cal_month.clear(Calendar.MINUTE);
				cal_month.clear(Calendar.SECOND);
				cal_month.clear(Calendar.MILLISECOND);
				
				cal_month.set(Calendar.DAY_OF_MONTH, 1);

				StartOfMonth = cal_week.getTimeInMillis();

				cal_month.add(Calendar.MONTH, 1);
				EndOfMonth = cal_month.getTimeInMillis();
				
		}
		
		public boolean isThisWeek(long now){
				if(now < EndOfWeek && now >= StartOfWeek){
						return true;
				}
				else
						return false;
		}
		public boolean isThisMonth(long now){
				if(now < EndOfMonth && now >= StartOfMonth){
						return true;
				}
				else
						return false;
		}

}