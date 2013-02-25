import java.util.HashMap;


public class GameStatistics {
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
	int PTS;  // total score
	String name; // team name

	/**
	 * default constructor
	 */
	public GameStatistics() {
		FGM = 0;
		FGA = 0;
		FG3M = 0;
		FG3A = 0;
		FTM = 0;
		FTA = 0;
		OREB = 0;
		DREB = 0;
		AST = 0;
		STL = 0;
		BLK = 0;
		TOV = 0;
		PF = 0;
		PTS = 0;
	}

	public GameStatistics(HashMap<String, Integer> columns, String[] row, String name) {
		this.name = name;
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
		PTS = Integer.parseInt(row[columns.get("PTS")]);
	}

	public void addStats(Game game, boolean isHome) {
		if (isHome) {
			FGM += game.homeStats.FGM;
			FGA += game.homeStats.FGA;
			FG3M += game.homeStats.FG3M;
			FG3A += game.homeStats.FG3A;
			FTM += game.homeStats.FTM;
			FTA += game.homeStats.FTA;
			OREB += game.homeStats.OREB;
			DREB += game.homeStats.DREB;
			AST += game.homeStats.AST;
			STL += game.homeStats.STL;
			BLK += game.homeStats.BLK;
			TOV += game.homeStats.TOV;
			PF += game.homeStats.PF;
			PTS += game.homeStats.PTS;
		}
		else {
			FGM += game.roadStats.FGM;
			FGA += game.roadStats.FGA;
			FG3M += game.roadStats.FG3M;
			FG3A += game.roadStats.FG3A;
			FTM += game.roadStats.FTM;
			FTA += game.roadStats.FTA;
			OREB += game.roadStats.OREB;
			DREB += game.roadStats.DREB;
			AST += game.roadStats.AST;
			STL += game.roadStats.STL;
			BLK += game.roadStats.BLK;
			TOV += game.roadStats.TOV;
			PF += game.roadStats.PF;
			PTS += game.roadStats.PTS;
		}
	}

	public void subtractStats(Game game, boolean isHome) {
		if (isHome) {
			FGM -= game.homeStats.FGM;
			FGA -= game.homeStats.FGA;
			FG3M -= game.homeStats.FG3M;
			FG3A -= game.homeStats.FG3A;
			FTM -= game.homeStats.FTM;
			FTA -= game.homeStats.FTA;
			OREB -= game.homeStats.OREB;
			DREB -= game.homeStats.DREB;
			AST -= game.homeStats.AST;
			STL -= game.homeStats.STL;
			BLK -= game.homeStats.BLK;
			TOV -= game.homeStats.TOV;
			PF -= game.homeStats.PF;
			PTS -= game.homeStats.PTS;
		}
		else {
			FGM -= game.roadStats.FGM;
			FGA -= game.roadStats.FGA;
			FG3M -= game.roadStats.FG3M;
			FG3A -= game.roadStats.FG3A;
			FTM -= game.roadStats.FTM;
			FTA -= game.roadStats.FTA;
			OREB -= game.roadStats.OREB;
			DREB -= game.roadStats.DREB;
			AST -= game.roadStats.AST;
			STL -= game.roadStats.STL;
			BLK -= game.roadStats.BLK;
			TOV -= game.roadStats.TOV;
			PF -= game.roadStats.PF;
			PTS -= game.roadStats.PTS;
		}
	}

	public GameStatistics calcAverage(int gamesPlayed) {
		GameStatistics stats = new GameStatistics();
		stats.FGM = FGM / gamesPlayed;
		stats.FGA = FGA / gamesPlayed;
		stats.FG3M = FG3M / gamesPlayed;
		stats.FG3A = FG3A / gamesPlayed;
		stats.FTM = FTM / gamesPlayed;
		stats.FTA = FTA / gamesPlayed;
		stats.OREB = OREB / gamesPlayed;
		stats.DREB = DREB / gamesPlayed;
		stats.AST = AST / gamesPlayed;
		stats.STL = STL / gamesPlayed;
		stats.BLK = BLK / gamesPlayed;
		stats.TOV = TOV / gamesPlayed;
		stats.PF = PF / gamesPlayed;
		stats.PTS = PTS / gamesPlayed;

		return stats;
	}

	public static String gameStatsHeader() {
		StringBuffer buff = new StringBuffer();
		buff.append("Team,");
		buff.append("PTS,");
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

	/**
	 * Return only major team stats for scor predictor
	 * @return
	 */
	public String getMajorTeamStats() {
		StringBuffer buff = new StringBuffer();
		buff.append(FGM).append(",");          // field goals made
		buff.append(FG3M).append(",");         // 3-point field goals made
		buff.append(FTM).append(",");          // tree throws made
		buff.append(OREB + DREB).append(",");  // total rebounds
		buff.append(AST).append(",");          // assists
		buff.append(STL).append(",");          // steals
		buff.append(BLK).append(",");          // blocks
		buff.append(TOV).append(",");          // turn overs
		buff.append(PTS);                      // game score
		return buff.toString();
	}

	public int getTotalScore() {
		return PTS;
	}

	public String toString() {
		StringBuffer buff = new StringBuffer();
		buff.append(name).append(",");
		buff.append(PTS).append(",");
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