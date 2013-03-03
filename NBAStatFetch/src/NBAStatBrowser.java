import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import com.google.gson.Gson;

/**
 * NBAStatBrowser.java
 * 
 * Retrieves individual game data from NBA.com by communicating via Gson
 * utility with internal database
 * 
 * @author Michelle Shu with help from my dad, Yueming Xu
 */

public class NBAStatBrowser {

	static HashMap<String, Game> games;  // hash on Game_ID, each game need to be updated twice

	public static void main(String[] args) throws FileNotFoundException {
		String season = "2011-12";  // specify a season to browse and save game stats
		String outFile = "data/2012-RAW.csv";

		games = new HashMap<String, Game>();
		NBAData teamData = getTeamBaseStats(season);
		String[] teamIDs = getTeamIDs(teamData);
		for (String t : teamIDs) {
			System.out.println("Collect games for team " + t);
			NBAData gameData = getTeamGameStats(season, t);
			collectGameStats(gameData);
		}
		System.out.println("Wrote games for " + teamIDs.length + " teams.");
		PrintWriter nbaFile = new PrintWriter(new File(outFile));
		nbaFile.println(Game.gameHeader());
		for (Game g : getSortedGameList()) {
			nbaFile.println(g.toString());
		}
		nbaFile.close();
		System.out.println("Done");
	}

	/**
	 * Sort the collected games in ascending order by game date
	 */
	public static ArrayList<Game> getSortedGameList() {
		ArrayList<Game> gameList = new ArrayList<Game>(games.values());
		// sort game list by game date
		Collections.sort(gameList, new Comparator<Game>() {
			public int compare(Game g1, Game g2) {
				if (g1.gameDate.before(g2.gameDate)) {
					return -1;
				} else if (g1.gameDate.after(g2.gameDate)) {
					return 1;
				} else {
					return 0;
				}
			}
		});

		return gameList;
	}

	
	/**
	 * Browse list of NBA teams in a specified season.
	 * Result contains base statistics of a team. Parameters can be used for more advanced stats.
	 * @param season , e.g., 2012-13
	 * @return NBA league dash team stats
	 */
	public static NBAData getTeamBaseStats(String season) {
		String serverUrl = "http://stats.nba.com/stats/leaguedashteamstats?Season=" + season +
				"&SeasonType=Regular+Season&MeasureType=Base&PerMode=Totals&PlusMinus=N&PaceAdjust=N&Rank=N&Outcome=&Location=&Month=0&SeasonSegment=&DateFrom=&DateTo=&OpponentTeamID=0&VsConference=&VsDivision=&GameSegment=&Period=0&LastNGames=0&GameScope=&PlayerExperience=&PlayerPosition=&StarterBench=";
		String resp = HttpUtil.getData(serverUrl);
		return new Gson().fromJson(resp, NBAData.class);
	}

	/**
	 * Extract list of team IDs from NBA team stat resultset
	 * @param data NBA league dash team stats
	 * @return
	 */
	static String[] getTeamIDs(NBAData data) {
		ResultSet[] resultSets = data.getResultSets();
		String[][] rowSet = resultSets[0].getRowSet();  // result set contains list of teams

		String[] teamIDs = new String[rowSet.length];
		int i = 0;
		for (String[] row : rowSet) {
			teamIDs[i++] = row[0];  // first field of each row is the team ID
								    // look at headers to identify other columns
		}
		return teamIDs;
	}

	/**
	 * Browse list of games played by a team in a specified season
	 * 
	 * @param season e.g., 2012-13
	 * @param teamID e.g., 1610612761
	 * @return NBA team game log
	 */
	public static NBAData getTeamGameStats(String season, String teamID) {
		String serverUrl = "http://stats.nba.com/stats/teamgamelog?Season=" + season +
				"&SeasonType=Regular+Season&PlayerID=&TeamID=" + teamID;
		String resp = HttpUtil.getData(serverUrl);
		return new Gson().fromJson(resp, NBAData.class);
	}

	/**
	 * Add or update a game using the NBA game result set
	 * @param data NBA team game log
	 */
	public static void collectGameStats(NBAData data) {
		ResultSet[] resultSets = data.getResultSets();
		String[] headers = resultSets[0].getHeaders();  // column names
		String[][] rowSet = resultSets[0].getRowSet();  // result set contains list of games

		HashMap<String, Integer> columns = new HashMap<String, Integer>();  // maps column names to column index
		for (int i = 0; i < headers.length; i++) {
			columns.put(headers[i], i);
		}

		for (String[] row : rowSet) {
			String gameId = row[columns.get("Game_ID")];
			Game g = games.get(gameId);
			if (g != null) {
				g.updateGame(columns, row);
			}
			else {
				games.put(gameId, new Game(gameId, columns, row));
			}
		}
	}

	public static class NBAData {
		public String getResource() {
			return resource;
		}
		public void setResource(String resource) {
			this.resource = resource;
		}
		public HashMap<String, String> getParameters() {
			return parameters;
		}
		public void setParameters(HashMap<String, String> parameters) {
			this.parameters = parameters;
		}
		public ResultSet[] getResultSets() {
			return resultSets;
		}
		public void setResultSets(ResultSet[] resultSets) {
			this.resultSets = resultSets;
		}
		private String resource;
		private HashMap<String, String> parameters;
		private ResultSet[] resultSets;
	}

	public static class ResultSet {
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String[] getHeaders() {
			return headers;
		}
		public void setHeaders(String[] headers) {
			this.headers = headers;
		}
		public String[][] getRowSet() {
			return rowSet;
		}
		public void setRowSet(String[][] rowSet) {
			this.rowSet = rowSet;
		}
		private String name;
		private String[] headers;
		private String[][] rowSet;
	}
}
