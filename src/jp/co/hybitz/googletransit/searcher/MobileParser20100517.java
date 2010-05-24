/**
 * Copyright (C) 2010 Hybitz.co.ltd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * 
 */
package jp.co.hybitz.googletransit.searcher;

import java.io.IOException;
import java.io.InputStream;

import jp.co.hybitz.googletransit.model.Time;
import jp.co.hybitz.googletransit.model.TimeAndPlace;
import jp.co.hybitz.googletransit.model.TimeType;
import jp.co.hybitz.googletransit.model.Transit;
import jp.co.hybitz.googletransit.model.TransitDetail;
import jp.co.hybitz.googletransit.model.TransitResult;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * @author ichy <ichylinux@gmail.com>
 */
class MobileParser20100517 implements TransitParser {
    private XmlPullParser parser;
    private TransitResult result = new TransitResult();
	private Transit transit;
	private TransitDetail transitDetail;
	
	public MobileParser20100517(XmlPullParser parser) {
	    this.parser = parser;
	}

	public TransitResult parse(InputStream in) throws XmlPullParserException, IOException {
		parser.setInput(in, null);
		
		boolean stopParsing = false;
		int eventType = parser.getEventType();
		while (true) {
			switch (eventType) {
			case XmlPullParser.START_TAG :
				break;
			case XmlPullParser.END_TAG :
				break;
			case XmlPullParser.TEXT :
				String text = parser.getText().trim();
				stopParsing = handleText(text);
				break;
			}
			
			if (stopParsing) {
			    break;
			}

			eventType = parser.next();
			if (eventType == XmlPullParser.END_DOCUMENT) {
			    break;
			}
		}

		return result;
	}
	
	/**
	 * テキスト部分を解析します。
	 * 
	 * @param text
	 * @return true：必要な情報までパースした場合、false：パース続行
	 */
	private boolean handleText(String text) {
	    if (text.matches(".*～.* [0-9]{1,2}:[0-9]{2}発")) {
	        handleSummary(text, TimeType.DEPARTURE);
	    }
	    else if (text.matches(".*～.* [0-9]{1,2}:[0-9]{2}着")) {
            handleSummary(text, TimeType.ARRIVAL);
        }
	    else if (text.matches(".*～.* 終電")) {
            handleSummary(text, TimeType.LAST);
	    }
	    else if (text.matches(".*[0-9]*円.*")) {
	        handleTimeAndFare(text);
        }
        else if ("逆方向の経路を表示".equals(text)) {
            if (transit != null) {
                if (transitDetail != null) {
                    transit.addDetail(transitDetail);
                    transitDetail = null;
                }
                
                result.addTransit(transit);
            }
            transit = null;
            return true;
        } else if (text.length() > 0) {
            if (text.matches("[0-9]{1,2}:[0-9]{2}発 .*")) {
                handleDeparture(text);            }
            else if (text.matches("[0-9]{1,2}:[0-9]{2}着 .*")) {
                handleArrival(text);
            } else {
                handleRoute(text);
            }
        }
	    
	    return false;
	}
	
	private void handleSummary(String text, TimeType timeType) {
	    result.setTimeType(timeType);
	    
	    if (timeType == TimeType.LAST) {
	        String[] split = text.split("～");
	        result.setFrom(split[0].trim());
	        result.setTo(split[1].trim().split(" ")[0]);
	    }
	    else {
            String[] split = text.split("～");
            result.setFrom(split[0].trim());
            
            String[] split2 = split[1].trim().replaceAll("(発|着)", "").trim().split(" ");
            result.setTo(split2[0]);
            
            String[] split3 = split2[1].trim().split(":");
            result.setTime(new Time(split3[0], split3[1]));
	    }
	    
	}
	
	private void handleTimeAndFare(String text) {
        if (transit != null) {
            if (transitDetail != null) {
                transit.addDetail(transitDetail);
                transitDetail = null;
            }

            result.addTransit(transit);
        }
        transit = new Transit();
        transit.setTimeAndFare(text);
	}
	
	private void handleDeparture(String text) {
        String[] split = text.split("発 ");
        String[] splitTime = split[0].split(":");
        Time time = new Time(Integer.parseInt(splitTime[0]), Integer.parseInt(splitTime[1]));
        transitDetail.setDeparture(new TimeAndPlace(time, split[1]));
	}
	
	private void handleArrival(String text) {
        String[] split = text.split("着 ");
        String[] splitTime = split[0].split(":");
        Time time = new Time(Integer.parseInt(splitTime[0]), Integer.parseInt(splitTime[1]));
        transitDetail.setArrival(new TimeAndPlace(time, split[1]));
	}
	
	private void handleRoute(String text) {
		if (transit == null) {
			return;
		}
		
		if (transitDetail != null) {
			transit.addDetail(transitDetail);
		}
		
		transitDetail = new TransitDetail();
		transitDetail.setRoute(text);
	}
}
