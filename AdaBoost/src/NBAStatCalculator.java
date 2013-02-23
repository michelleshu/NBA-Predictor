import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;


public class NBAStatCalculator {
	static HashMap<String, Team> teams;
	static final int EARLY_SEASON = 5;

	static int getTeamCount() {
		return teams.size();
	}

	/**
	 * Read NBA game records of a specified season, 
	 * and store the game in both home-team and road-team records.
	 * 
	 * @param gameFile
	 * @param season
	 */
	static void readGameFile(String gameFile, int season) {
		BufferedReader reader = null;
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
		try {
			teams = new HashMap<String, Team>();
			reader = new BufferedReader(new FileReader(gameFile));
			String line;
			int gameCount = 0;
			while ((line = reader.readLine()) != null) {
				String[] tokens = line.split(",");
				if (tokens.length >= 5) {
					try {
						// parse data
						Date dt = fmt.parse(tokens[0]);
						String roadTeamName = tokens[1];
						int roadScore = Integer.parseInt(tokens[2]);
						String homeTeamName = tokens[3];
						int homeScore = Integer.parseInt(tokens[4]);

						// create new game
						Game game = new Game(roadTeamName, roadScore, homeTeamName, homeScore, dt);
						int gameSeason = game.getSeason();
						if (gameSeason < season) {
							continue;  // skip games of previous season
						}
						else if (gameSeason > season) {
							break;  // stop after all games of the specified season
						}
						gameCount++;

						// add road game
						Team roadTeam = teams.get(roadTeamName);
						if (null == roadTeam) {
							roadTeam = new Team(roadTeamName);
							teams.put(roadTeamName, roadTeam);
						}
						roadTeam.addRoadGame(game);

						// add home game
						Team homeTeam = teams.get(homeTeamName);
						if (null == homeTeam) {
							homeTeam = new Team(homeTeamName);
							teams.put(homeTeamName, homeTeam);
						}
						homeTeam.addHomeGame(game);
					} catch (Exception e) {
						LogHelper.logln("DEBUG", "Failed process NBA game record " + line);
					}
				}
			}
			LogHelper.logln("DEBUG", "Read " + gameCount + " games for season " + season);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception e) {}
			}
		}
	}

	static void writeGameStats(String statFile) {
		// calculate game stats
		for (Team team : teams.values()) {
			team.calcHomeStats();
			team.calcRoadStats();
		}

		// write stats according to home games
		PrintWriter out = null;
		try {
			out = new PrintWriter(new File(statFile));
			for (Team team : teams.values()) {
				LogHelper.logln("DEBUG", team.name + ":  " + team.getGamesPlayed());
				for (Game g : team.getHomeGames()) {
					// write csv line for each game, skip early season games
					GameStatistics stat = g.getStats();
					if (stat.getTenHomeAvg() > 0 && stat.getTenRoadAvg() > 0) {
						StringBuffer buff = new StringBuffer();
						buff.append(stat.seasonHomeAvg).append(",");
						buff.append(stat.seasonHomeOppAvg).append(",");
						buff.append(stat.seasonRoadAvg).append(",");
						buff.append(stat.seasonRoadOppAvg).append(",");
						buff.append(stat.tenHomeAvg).append(",");
						buff.append(stat.tenHomeOppAvg).append(",");
						buff.append(stat.tenRoadAvg).append(",");
						buff.append(stat.tenRoadOppAvg).append(",");
						buff.append(g.homeScore - g.roadScore).append(",");
						buff.append(g.homeScore + g.roadScore);
						out.println(buff.toString());
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	static public class Team {
		String name;
		ArrayList<Game> homeGames;
		ArrayList<Game> roadGames;

		public Team(String name) {
			this.name = name;
			homeGames = new ArrayList<Game>();
			roadGames = new ArrayList<Game>();
		}

		public void addHomeGame(Game game) {
			homeGames.add(game);
		}

		public void addRoadGame(Game game) {
			roadGames.add(game);
		}

		public ArrayList<Game> getHomeGames() {
			return homeGames;
		}

		public int getGamesPlayed() {
			return homeGames.size() + roadGames.size();
		}

		public void calcHomeStats() {
			int seasonTotal = 0;
			int seasonOppTotal = 0;
			int tenTotal = 0;
			int tenOppTotal = 0;
			int gamesPlayed = 0;
			for (int i = 0; i < homeGames.size(); i++) {
				Game g = homeGames.get(i);
				seasonTotal += g.homeScore;
				seasonOppTotal += g.roadScore;
				tenTotal += g.homeScore;
				tenOppTotal += g.roadScore;
				gamesPlayed++;
				if (gamesPlayed > 10) {
					Game pg = homeGames.get(i-10);
					tenTotal -= pg.homeScore;
					tenOppTotal -= pg.roadScore;
				}
				GameStatistics stat = g.getStats();
				stat.setSeasonHomeAvg(seasonTotal/gamesPlayed);
				stat.setSeasonHomeOppAvg(seasonOppTotal/gamesPlayed);
				if (gamesPlayed >= EARLY_SEASON) {
					int tenPlayed = Math.min(gamesPlayed, 10);
					stat.setTenHomeAvg(tenTotal/tenPlayed);
					stat.setTenHomeOppAvg(tenOppTotal/tenPlayed);
				}
			}
		}

		public void calcRoadStats() {
			int seasonTotal = 0;
			int seasonOppTotal = 0;
			int tenTotal = 0;
			int tenOppTotal = 0;
			int gamesPlayed = 0;
			for (int i = 0; i < roadGames.size(); i++) {
				Game g = roadGames.get(i);
				seasonTotal += g.roadScore;
				seasonOppTotal += g.homeScore;
				tenTotal += g.roadScore;
				tenOppTotal += g.homeScore;
				gamesPlayed++;
				if (gamesPlayed > 10) {
					Game pg = roadGames.get(i-10);
					tenTotal -= pg.roadScore;
					tenOppTotal -= pg.homeScore;
				}
				GameStatistics stat = g.getStats();
				stat.setSeasonRoadAvg(seasonTotal/gamesPlayed);
				stat.setSeasonRoadOppAvg(seasonOppTotal/gamesPlayed);
				if (gamesPlayed >= EARLY_SEASON) {
					int tenPlayed = Math.min(gamesPlayed, 10);
					stat.setTenRoadAvg(tenTotal/tenPlayed);
					stat.setTenRoadOppAvg(tenOppTotal/tenPlayed);
				}
			}
		}

	}

	static public class Game {
		String homeTeam;
		String roadTeam;
		int homeScore;
		int roadScore;
		Date gameDate;
		GameStatistics stats;

		public GameStatistics getStats() {
			return stats;
		}

		public Game(String roadTeam, int roadScore, String homeTeam, int homeScore, Date gameDate) {
			this.roadTeam = roadTeam;
			this.roadScore = roadScore;
			this.homeTeam = homeTeam;
			this.homeScore = homeScore;
			this.gameDate = gameDate;
			this.stats = new GameStatistics();
		}

		/**
		 * Calculate the season year, season starts in Oct, and ends in May,
		 * e.g., season 2007 starts in Oct 2007, and ends in May 2008
		 * @return
		 */
		public int getSeason() {
			Calendar cal = Calendar.getInstance();
			cal.setTime(gameDate);
			int year = cal.get(Calendar.YEAR);
			int month = cal.get(Calendar.MONTH);
			return month < 5 ? year-1 : year;
		}
	}

	static public class GameStatistics {
		public int getSeasonHomeAvg() {
			return seasonHomeAvg;
		}
		public void setSeasonHomeAvg(int seasonHomeAvg) {
			this.seasonHomeAvg = seasonHomeAvg;
		}
		public int getSeasonHomeOppAvg() {
			return seasonHomeOppAvg;
		}
		public void setSeasonHomeOppAvg(int seasonHomeOppAvg) {
			this.seasonHomeOppAvg = seasonHomeOppAvg;
		}
		public int getSeasonRoadAvg() {
			return seasonRoadAvg;
		}
		public void setSeasonRoadAvg(int seasonRoadAvg) {
			this.seasonRoadAvg = seasonRoadAvg;
		}
		public int getSeasonRoadOppAvg() {
			return seasonRoadOppAvg;
		}
		public void setSeasonRoadOppAvg(int seasonRoadOppAvg) {
			this.seasonRoadOppAvg = seasonRoadOppAvg;
		}
		public int getTenHomeAvg() {
			return tenHomeAvg;
		}
		public void setTenHomeAvg(int tenHomeAvg) {
			this.tenHomeAvg = tenHomeAvg;
		}
		public int getTenHomeOppAvg() {
			return tenHomeOppAvg;
		}
		public void setTenHomeOppAvg(int tenHomeOppAvg) {
			this.tenHomeOppAvg = tenHomeOppAvg;
		}
		public int getTenRoadAvg() {
			return tenRoadAvg;
		}
		public void setTenRoadAvg(int tenRoadAvg) {
			this.tenRoadAvg = tenRoadAvg;
		}
		public int getTenRoadOppAvg() {
			return tenRoadOppAvg;
		}
		public void setTenRoadOppAvg(int tenRoadOppAvg) {
			this.tenRoadOppAvg = tenRoadOppAvg;
		}

		int seasonHomeAvg;
		int seasonHomeOppAvg;
		int seasonRoadAvg;
		int seasonRoadOppAvg;
		int tenHomeAvg;
		int tenHomeOppAvg;
		int tenRoadAvg;
		int tenRoadOppAvg;
	}

	public static void main(String args[]) {
		LogHelper.setDebug(true);
		for (int season=2006; season < 2010; season++) {
			readGameFile("C:/work/workspace/AdaBoost/src/nba.csv", season);
			if (getTeamCount() > 0) {
				writeGameStats("C:/work/workspace/AdaBoost/src/nba-" + season + ".csv");
			}
		}
	}
}
