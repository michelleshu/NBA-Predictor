import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;

import com.google.gson.Gson;

public class NBAStatBrowser {

	static HashMap<String, Game> games;  // hash on Game_ID, each game need to be updated twice

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		String season = "2007-08";  // specify a season to browse and save game stats
		String outFile = "/Users/michelleshu/Documents/2013/CS74/Workspace/NBAStatFetch/07.csv";

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
	 * @return
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
	 * 
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

	public static class Game {
		String gameId;
		Date gameDate;
		String homeTeamId;
		String homeTeamName;
		int homeTeamScore;
		GameStatistics homeStats;
		String roadTeamId;
		String roadTeamName;
		int roadTeamScore;
		GameStatistics roadStats;

		public Game(String gameId, HashMap<String, Integer> columns, String[] row) {
			this.gameId = gameId;

			SimpleDateFormat fmt = new SimpleDateFormat("MMM dd, yyyy");
			String dateStr = row[columns.get("GAME_DATE")];
			try {
				gameDate = fmt.parse(dateStr);
			} catch (Exception e) {}

			updateGame(columns, row);
		}

		public void updateGame(HashMap<String, Integer> columns, String[] row) {
			String matchup = row[columns.get("MATCHUP")];
			String[] tokens = matchup.split(" ");
			if (matchup.indexOf("@") > 0) {
				roadTeamId = row[columns.get("Team_ID")];
				roadTeamName = tokens[0];
				roadTeamScore = Integer.parseInt(row[columns.get("PTS")]);
				roadStats = new GameStatistics(columns, row);
				homeTeamName = tokens[2];
			} else {
				homeTeamId = row[columns.get("Team_ID")];
				homeTeamName = tokens[0];
				homeTeamScore = Integer.parseInt(row[columns.get("PTS")]);
				homeStats = new GameStatistics(columns, row);
				roadTeamName = tokens[2];
			}
		}

		public static String gameHeader() {
			StringBuffer buff = new StringBuffer();
			buff.append("gameId,");
			buff.append("gameDate,");
			buff.append("homeTeamId,");
			buff.append("homeTeamName,");
			buff.append("homeTeamScore,");
			buff.append(GameStatistics.gameStatsHeader()).append(",");
			buff.append("roadTeamId,");
			buff.append("roadTeamName,");
			buff.append("roadTeamScore,");
			buff.append(GameStatistics.gameStatsHeader());
			return buff.toString();
		}

		public String toString() {
			StringBuffer buff = new StringBuffer();
			SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
			buff.append(gameId).append(",");
			buff.append(fmt.format(gameDate)).append(",");
			buff.append(homeTeamId).append(",");
			buff.append(homeTeamName).append(",");
			buff.append(homeTeamScore).append(",");
			buff.append(homeStats.toString()).append(",");
			buff.append(roadTeamId).append(",");
			buff.append(roadTeamName).append(",");
			buff.append(roadTeamScore).append(",");
			buff.append(roadStats.toString());
			return buff.toString();
		}
	}

	public static class GameStatistics {
		int FGM;  // field goals made
		int FGA;  // field goals attempted
		int FG3M; // 3-point field goals made
		int FG3A; // 3-point field goals attempted
		int FTM;  // free throws made
		int FTA;  // free throws attempted
		int OREB; // offensive rebounds
		int DREB; // defensive rebounds
		int AST;  // assists
		int STL;  // steals
		int BLK;  // blocks
		int TOV;  // turn-overs
		int PF;   // personal fouls

		public GameStatistics(HashMap<String, Integer> columns, String[] row) {
			FGM = Integer.parseInt(row[columns.get("FGM")]);
			FGA = Integer.parseInt(row[columns.get("FGA")]);
			FG3M = Integer.parseInt(row[columns.get("FG3M")]);
			FG3A = Integer.parseInt(row[columns.get("FG3A")]);
			FTM = Integer.parseInt(row[columns.get("FTM")]);
			FTA = Integer.parseInt(row[columns.get("FTA")]);
			OREB = Integer.parseInt(row[columns.get("OREB")]);
			DREB = Integer.parseInt(row[columns.get("DREB")]);
			AST = Integer.parseInt(row[columns.get("AST")]);
			STL = Integer.parseInt(row[columns.get("STL")]);
			BLK = Integer.parseInt(row[columns.get("BLK")]);
			TOV = Integer.parseInt(row[columns.get("TOV")]);
			PF = Integer.parseInt(row[columns.get("PF")]);
		}

		public static String gameStatsHeader() {
			StringBuffer buff = new StringBuffer();
			buff.append("FGM,");
			buff.append("FGA,");
			buff.append("FG3M,");
			buff.append("FG3A,");
			buff.append("FTM,");
			buff.append("FTA,");
			buff.append("OREB,");
			buff.append("DREB,");
			buff.append("AST,");
			buff.append("STL,");
			buff.append("BLK,");
			buff.append("TOV,");
			buff.append("PF");
			return buff.toString();
		}

		public String toString() {
			StringBuffer buff = new StringBuffer();
			buff.append(FGM).append(",");
			buff.append(FGA).append(",");
			buff.append(FG3M).append(",");
			buff.append(FG3A).append(",");
			buff.append(FTM).append(",");
			buff.append(FTA).append(",");
			buff.append(OREB).append(",");
			buff.append(DREB).append(",");
			buff.append(AST).append(",");
			buff.append(STL).append(",");
			buff.append(BLK).append(",");
			buff.append(TOV).append(",");
			buff.append(PF);
			return buff.toString();
		}
	}
}
