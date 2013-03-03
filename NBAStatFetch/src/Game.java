import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * Game.java
 * A class to represent data for a single game
 */

public class Game {
	
	/** Getters and setters */
	
	public void setSeasonHomeAvg(GameStatistics seasonHomeAvg) {
		this.seasonHomeAvg = seasonHomeAvg;
	}

	public void setSeasonHomeOppAvg(GameStatistics seasonHomeOppAvg) {
		this.seasonHomeOppAvg = seasonHomeOppAvg;
	}

	public void setTenHomeAvg(GameStatistics tenHomeAvg) {
		this.tenHomeAvg = tenHomeAvg;
	}

	public void setTenHomeOppAvg(GameStatistics tenHomeOppAvg) {
		this.tenHomeOppAvg = tenHomeOppAvg;
	}

	public void setSeasonRoadAvg(GameStatistics seasonRoadAvg) {
		this.seasonRoadAvg = seasonRoadAvg;
	}

	public void setSeasonRoadOppAvg(GameStatistics seasonRoadOppAvg) {
		this.seasonRoadOppAvg = seasonRoadOppAvg;
	}

	public void setTenRoadAvg(GameStatistics tenRoadAvg) {
		this.tenRoadAvg = tenRoadAvg;
	}

	public void setTenRoadOppAvg(GameStatistics tenRoadOppAvg) {
		this.tenRoadOppAvg = tenRoadOppAvg;
	}

	String gameId;
	Date gameDate;
	String homeTeamId;
	GameStatistics homeStats;
	GameStatistics seasonHomeAvg;
	GameStatistics seasonHomeOppAvg;
	GameStatistics tenHomeAvg;
	GameStatistics tenHomeOppAvg;
	String roadTeamId;
	GameStatistics roadStats;
	GameStatistics seasonRoadAvg;
	GameStatistics seasonRoadOppAvg;
	GameStatistics tenRoadAvg;
	GameStatistics tenRoadOppAvg;

	/** Constructor for NBADataParser */
	public Game(String gameId, HashMap<String, Integer> columns, String[] row) {
		this.gameId = gameId;

		SimpleDateFormat fmt = new SimpleDateFormat("MMM dd, yyyy");
		String dateStr = row[columns.get("GAME_DATE")];
		try {
			gameDate = fmt.parse(dateStr);
		} catch (Exception e) {}

		updateGame(columns, row);
	}

	/** Update game from NBADataParser */
	public void updateGame(HashMap<String, Integer> columns, String[] row) {
		String matchup = row[columns.get("MATCHUP")];
		String[] tokens = matchup.split(" ");
		if (matchup.indexOf("@") > 0) {
			roadTeamId = row[columns.get("Team_ID")];
			String name = tokens[0];
			roadStats = new GameStatistics(columns, row, name);
		} else {
			homeTeamId = row[columns.get("Team_ID")];
			String name = tokens[0];
			homeStats = new GameStatistics(columns, row, name);
		}
	}

	/** Constructor for NBAStatCalculator */
	public Game(Date gameDate) {
		this.gameDate = gameDate;
	}

	/** Update game from NBAStatCalculator */
	public void updateGame(HashMap<String, Integer> columns, String[] row, boolean isHome) {
		if (isHome) {
			String name = row[columns.get("Team")];
			homeStats = new GameStatistics(columns, row, name);
		} else {
			String name = row[columns.get("Team")];
			roadStats = new GameStatistics(columns, row, name);
		}
	}
	
	public String getHomeTeam() {
		return homeStats.name;
	}
	
	public String getRoadTeam() {
		return roadStats.name;
	}

	public static String gameHeader() {
		StringBuffer buff = new StringBuffer();
		buff.append("GameId,");
		buff.append("GameDate,");
		buff.append("HomeTeamId,");
		buff.append(GameStatistics.gameStatsHeader()).append(",");
		buff.append("RoadTeamId,");
		buff.append(GameStatistics.gameStatsHeader());
		return buff.toString();
	}

	public String toString() {
		StringBuffer buff = new StringBuffer();
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
		buff.append(gameId).append(",");
		buff.append(fmt.format(gameDate)).append(",");
		buff.append(homeTeamId).append(",");
		buff.append(homeStats.toString()).append(",");
		buff.append(roadTeamId).append(",");
		buff.append(roadStats.toString());
		return buff.toString();
	}
}