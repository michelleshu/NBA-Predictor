import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class NBADataParser {

	private static String[] extractGames(String html) {
		String tableContent = extractData(html, "value=\"Get Scores\"", null);
		if (tableContent != null) {
			ArrayList<String> gameList = new ArrayList<String>();
			int startIndex = 0;
			while ((startIndex = tableContent.indexOf("<tr>")) >= 0) {
				gameList.add(extractData(tableContent, "<tr>", "</tr>"));
				tableContent = tableContent.substring(startIndex + 4);
			}
			if (gameList.size() > 0) {
				String[] games = new String[gameList.size()];
				gameList.toArray(games);
				return games;
			}
		}
		return null;
	}
	
	/**
	 * Return comma-delimited line as RoadTeam,score,HomeTeam,score
	 * @param game
	 * @return
	 */
	private static String parseGame(String game) {
		int startIndex = 0;
		StringBuffer buff = new StringBuffer();
		while ((startIndex = game.indexOf("<td>")) >= 0) {
			String field = extractData(game, "<td>", "</td>");
			if (!"at".equals(field)) {
				int index = field.indexOf("teamyear");
				if (index >= 0) {
					field = extractData(field.substring(index), "tm=", "&lg");
				}
				if (buff.length() > 0) buff.append(',');
				buff.append(field);
			}
			game = game.substring(startIndex + 4);
		}
		return buff.toString();
	}
	
	/**
	 * Extract part of the data between a start string and an end string.
	 * If end-string is null, return the content after the start-string.
	 * 
	 * @param data
	 * @param start
	 * @param end
	 * @return
	 */
	private static String extractData(String data, String start, String end) {
		int startIdx = data.indexOf(start);
		int endIdx = 0;
		if (startIdx >= 0 && end != null) {
			endIdx = data.indexOf(end, startIdx + start.length());
		}
		if (startIdx >= 0) {
			if (endIdx > 0) {
				return data.substring(startIdx+start.length(), endIdx);
			} else {
				return data.substring(startIdx+start.length());
			}
		} else {
			return null;
		}
	}

	/**
	 * Returns the next NBA date, skip off-season
	 * @param d
	 * @return
	 */
	private static Calendar nextNBADate(Calendar cal) {
		cal.add(Calendar.DATE, 1);
		int month = cal.get(Calendar.MONTH);
		if (month > 4 && month < 9) {
			cal.set(Calendar.MONTH, 9);
			cal.set(Calendar.DATE, 1);
		}
		return cal;
	}
	
	private static String[] queryGames(int year, int month, int date) {
		String serverUrl = "http://www.databasebasketball.com/boxscores/dailyscores.htm?lg=N";
		StringBuffer params = new StringBuffer("m=");
		params.append(month);
		params.append("&d=").append(date);
		params.append("&y=").append(year);
		params.append("&submit=Get+Scores");
		String response = HttpUtil.postParameters(serverUrl, params.toString());
		String[] games = extractGames(response);
		if (games != null) {
			String[] gameCsv = new String[games.length];
			for (int i = 0; i < games.length; i++) {
				gameCsv[i] = parseGame(games[i]);
			}
			return gameCsv;
		}
		else {
			return null;
		}
	}

	public static void main(String[] args) throws Exception {
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
		Date dt = fmt.parse("2006-10-01");
		PrintWriter nbaFile = new PrintWriter(new File("c:/temp/nba.txt"));

		Calendar cal = Calendar.getInstance();
		cal.setTime(dt);
		for (int i = 1; i < 1050; i++) {
			nextNBADate(cal);
			String dtStr = fmt.format(cal.getTime());
			String[] gameCsv = queryGames(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DATE));
			if (gameCsv != null) {
				for (String g : gameCsv) {
					nbaFile.println(dtStr + "," + g);
				}
			}
			System.out.println(dtStr + " " + (gameCsv == null ? 0 : gameCsv.length));
		}
		nbaFile.close();

		/*
		String serverUrl = "http://www.databasebasketball.com/boxscores/dailyscores.htm?lg=N";
		String params = "m=5&d=10&y=2010&submit=Get+Scores";
		String response = HttpUtil.postParameters(serverUrl, params);
		String[] games = extractGames(response);
		for (String g : games) {
			System.out.println(parseGame(g));
		}
		*/
	}

}
