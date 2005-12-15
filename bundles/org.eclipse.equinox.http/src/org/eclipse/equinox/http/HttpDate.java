/*******************************************************************************
 * Copyright (c) 1999, 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.http;

import java.util.*;

/**
 * <b>HTTPDate</b> provides several services with regards to dates and date formatting.
 */
public class HttpDate {

	protected Calendar time;
	private boolean valid = true;

	private static final String[] MONTH_NAMES = {HttpMsg.Jan_1, HttpMsg.Feb_2, HttpMsg.Mar_3, HttpMsg.Apr_4, HttpMsg.May_5, HttpMsg.Jun_6, HttpMsg.Jul_7, HttpMsg.Aug_8, HttpMsg.Sep_9, HttpMsg.Oct_10, HttpMsg.Nov_11, HttpMsg.Dec_12};
	private static final String[] WEEK_DAYS = {HttpMsg.Sun_13, HttpMsg.Mon_14, HttpMsg.Tue_15, HttpMsg.Wed_16, HttpMsg.Thu_17, HttpMsg.Fri_18, HttpMsg.Sat_19};

	private static TimeZone gmt = null;

	/**
	 * Constructs an HTTPDate object representing the current time.
	 */
	public HttpDate() { // Now...
		if (gmt == null) {
			gmt = TimeZone.getTimeZone("GMT"); //$NON-NLS-1$
		}
		time = new GregorianCalendar(gmt);
	}

	/**
	 * Constructs an HTTPDate object from the passed long.
	 * @param itime the number of uSeconds since the epoch.
	 */
	public HttpDate(long iTime) { // Raw mill seconds
		this();
		time.setTime(new Date(iTime));
	}

	/**
	 * Constructs an HTTPDate object from an RFC compliant string.
	 * @param iString the time in a formatted string.
	 */
	public HttpDate(String iString) { // RFC2068 3.3.1 compliant
		this();
		if (iString.indexOf(",") > 0) //$NON-NLS-1$
		{
			if (iString.indexOf("-") > 0) //$NON-NLS-1$
			{
				parseRFC1036(iString);
			} else {
				parseRFC1123(iString);
			}
		} else { //asctime type
			parseASC(iString);
		}
	}

	/**
	 * Constructs an HTTPDate object from the passed long.
	 * @param itime the number of uSeconds since the epoch.
	 */
	public HttpDate(Date iTime) { // Raw mill seconds
		this();
		time.setTime(iTime);
	}

	/**
	 * Returns the Date/Time time as an RFC 1123 formatted String
	 */
	public static String format(long time) {
		return (new HttpDate(time).toString());
	}

	protected void formatDoubleDigit(StringBuffer buff, int val) {
		if (val < 10) {
			buff.append('0');
		}

		buff.append(val);
	}

	/**
	 * Returns the time represented as a long.  This represents the number
	 * of uSeconds since the epoch.
	 * @return the time represented as a long.
	 */
	public long getAsLong() {
		return (time.getTime().getTime());
	}

	/**
	 * Returns the time represented as a string.
	 * @return the time represented as a string.
	 */
	public String getAsString() {
		return (toString());
	}

	/**
	 * Returns the day of the month of the internal time, starting at 1
	 * @return the day of the month.
	 */
	public int getDay() {
		return (time.get(Calendar.DATE));
	}

	/**
	 * Returns the day of the week of the internal time.
	 * The number is in the range of 0 to 6 where 0 represents Sunday.
	 * @return  the day of the week.
	 */
	public int getDayOfWeek() {
		return (time.get(Calendar.DAY_OF_WEEK) - 1);
	}

	/**
	 * Returns the hour of the day of the internal time. 24 Hour time
	 * @return  the hour.
	 */
	public int getHour() {
		return (time.get(Calendar.HOUR_OF_DAY));
	}

	/**
	 * Returns the minute of the hour of the internal time.
	 * @return  the minute.
	 */
	public int getMin() {
		return (time.get(Calendar.MINUTE));
	}

	/**
	 * Returns the month of the year of the internal time.
	 * The number is in the range of 0 to 11 where 0 represents January.
	 * @return  the month of the year.
	 */
	public int getMonth() {
		return (time.get(Calendar.MONTH));
	}

	/**
	 * Returns the second of the minute of the internal time.
	 * @return  the second.
	 */
	public int getSec() {
		return (time.get(Calendar.SECOND));
	}

	/**
	 * Returns the year of the internal time.
	 * @return  the year.
	 */
	public int getYear() {
		return (time.get(Calendar.YEAR));
	}

	/**
	 * Checks wether or not the Date this object represents is valid.
	 *  It would be Invalid if the string used to construct the object was a NON
	 * RFC 1123, 1036 or ASC Time conforming Date String.
	 * @return true if this object represents a REAL date.
	 */
	public boolean isValid() {
		return (valid);
	}

	protected int locateMonth(String monthString) throws NumberFormatException {
		for (int i = 0; i < 12; i++) {
			if (MONTH_NAMES[i].equals(monthString)) {
				return (i);
			}
		}
		throw new NumberFormatException("Invalid month: " + monthString); //$NON-NLS-1$
	}

	/**
	 * Returns the current Date/Time as an RFC ASC Time formatted String
	 */
	public static String now() {
		return (new HttpDate().toString());
	}

	protected void parseASC(String str) {
		//ASCTIMEFMT  = "EEE MMM dD HH:MM:SS YYYY"
		//               012345678901234567890123

		int day, month, year, hour, min, sec;

		parsedate: try {
			/* ignore day */

			if (str.charAt(3) != ' ') {
				break parsedate;
			}

			month = locateMonth(str.substring(4, 7));

			if (str.charAt(7) != ' ') {
				break parsedate;
			}

			if (str.charAt(8) == ' ') {
				day = Integer.parseInt(str.substring(9, 10));
			} else {
				day = Integer.parseInt(str.substring(8, 10));
			}

			if (str.charAt(10) != ' ') {
				break parsedate;
			}

			hour = Integer.parseInt(str.substring(11, 13));

			if (str.charAt(13) != ':') {
				break parsedate;
			}

			min = Integer.parseInt(str.substring(14, 16));

			if (str.charAt(16) != ':') {
				break parsedate;
			}

			sec = Integer.parseInt(str.substring(17, 19));

			if (str.charAt(19) != ' ') {
				break parsedate;
			}

			year = Integer.parseInt(str.substring(20));

			time.set(year, month, day, hour, min, sec);
			return;
		} catch (NumberFormatException e) {
		}

		valid = false;
	}

	protected void parseRFC1036(String str) {
		//RFC1036DATEFMT = "EEEE, DD-MMM-YY HH:MM:SS ZZZ"
		//                        0123456789012345678901

		int day, month, year, hour, min, sec;
		parsedate: try {
			/* skip past day */

			int i = str.indexOf(", "); //$NON-NLS-1$

			if (i == -1) {
				break parsedate;
			}

			str = str.substring(i + 2);

			day = Integer.parseInt(str.substring(0, 2));

			if (str.charAt(2) != '-') {
				break parsedate;
			}

			month = locateMonth(str.substring(3, 6));

			if (str.charAt(6) != '-') {
				break parsedate;
			}

			year = Integer.parseInt(str.substring(7, 9));
			year += (year < 70) ? 2000 : 1900; /* y2k window */

			if (str.charAt(9) != ' ') {
				break parsedate;
			}

			hour = Integer.parseInt(str.substring(10, 12));

			if (str.charAt(12) != ':') {
				break parsedate;
			}

			min = Integer.parseInt(str.substring(13, 15));

			if (str.charAt(15) != ':') {
				break parsedate;
			}

			sec = Integer.parseInt(str.substring(16, 18));

			if (str.charAt(18) != ' ') {
				break parsedate;
			}

			time.set(year, month, day, hour, min, sec);
			return;
		} catch (NumberFormatException e) {
		}

		valid = false;
	}

	protected void parseRFC1123(String str) {
		//RFC1123DATEFMT = "EEE, DD MMM YYYY HH:MM:SS ZZZ"
		//                  01234567890123456789012345678

		int day, month, year, hour, min, sec;

		parsedate: try {
			/* ignore day */

			if ((str.charAt(3) != ',') || (str.charAt(4) != ' ')) {
				break parsedate;
			}

			day = Integer.parseInt(str.substring(5, 7));

			if (str.charAt(7) != ' ') {
				break parsedate;
			}

			month = locateMonth(str.substring(8, 11));

			if (str.charAt(11) != ' ') {
				break parsedate;
			}

			year = Integer.parseInt(str.substring(12, 16));

			if (str.charAt(16) != ' ') {
				break parsedate;
			}

			hour = Integer.parseInt(str.substring(17, 19));

			if (str.charAt(19) != ':') {
				break parsedate;
			}

			min = Integer.parseInt(str.substring(20, 22));

			if (str.charAt(22) != ':') {
				break parsedate;
			}

			sec = Integer.parseInt(str.substring(23, 25));

			if (str.charAt(25) != ' ') {
				break parsedate;
			}

			time.set(year, month, day, hour, min, sec);
			return;
		} catch (NumberFormatException e) {
		}

		valid = false;
	}

	/**
	 * Returns the time represented as an RFC1123 string.
	 * @return the time represented as a string.
	 */
	public String toString() {
		StringBuffer buff = new StringBuffer(30);
		buff.append(WEEK_DAYS[getDayOfWeek()]);
		buff.append(", "); //$NON-NLS-1$
		formatDoubleDigit(buff, getDay());
		buff.append(' ');
		buff.append(MONTH_NAMES[getMonth()]);
		buff.append(' ');
		buff.append(getYear());
		buff.append(' ');

		formatDoubleDigit(buff, getHour());
		buff.append(':');
		formatDoubleDigit(buff, getMin());
		buff.append(':');
		formatDoubleDigit(buff, getSec());
		buff.append(' ');
		buff.append(time.getTimeZone().getID());

		return (buff.toString());
	}
}
