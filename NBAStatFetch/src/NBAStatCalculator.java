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
	static void readGameFile(String gameFile) {
		BufferedReader reader = null;
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
		try {
			teams = new HashMap<String, Team>();
			reader = new BufferedReader(new FileReader(gameFile));
			String line;
			int gameCount = 0;
			boolean header = true;
			HashMap<String, Integer> homeHeader = new HashMap<String, Integer>();
			HashMap<String, Integer> roadHeader = new HashMap<String, Integer>();
			while ((line = reader.readLine()) != null) {
				String[] tokens = line.split(",");
				if (tokens.length >= 34) {
					if (header) {  // process header
						header = false;
						for (int i = 3; i < 18; i++) {
							homeHeader.put(tokens[i], i);
							roadHeader.put(tokens[i+16], i+16);
						}
						continue;
					}
					try {
						// create new game record
						Date dt = fmt.parse(tokens[1]);
						Game game = new Game(dt);
						game.updateGame(homeHeader, tokens, true);
						game.updateGame(roadHeader, tokens, false);
						gameCount++;

						// add road game
						String roadTeamName = game.getRoadTeam();
						Team roadTeam = teams.get(roadTeamName);
						if (null == roadTeam) {
							roadTeam = new Team(roadTeamName);
							teams.put(roadTeamName, roadTeam);
						}
						roadTeam.addRoadGame(game);

						// add home game
						String homeTeamName = game.getHomeTeam();
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
			LogHelper.logln("DEBUG", "Read " + gameCount + " from " + gameFile);
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

	static void writeGameStats(String statFile, boolean useLastTen) {
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
					if (g.tenHomeAvg != null && g.tenRoadAvg != null) {
						StringBuffer buff = new StringBuffer();
						if (useLastTen) {
							buff.append(g.tenHomeAvg.getMajorTeamStats()).append(",");         // home team 10-game average
							buff.append(g.tenHomeOppAvg.getMajorTeamStats()).append(",");      // home team's opponent 10-game average
							buff.append(g.tenRoadAvg.getMajorTeamStats()).append(",");         // road team 10-game average
							buff.append(g.tenRoadOppAvg.getMajorTeamStats()).append(",");      // road team's opponent 10-game average
						} else {
							buff.append(g.seasonHomeAvg.getMajorTeamStats()).append(",");
							buff.append(g.seasonHomeOppAvg.getMajorTeamStats()).append(",");
							buff.append(g.seasonRoadAvg.getMajorTeamStats()).append(",");
							buff.append(g.seasonRoadOppAvg.getMajorTeamStats()).append(",");
						}
						buff.append(g.homeStats.getTotalScore() - g.roadStats.getTotalScore()).append(",");
						buff.append(g.homeStats.getTotalScore() + g.roadStats.getTotalScore());
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
			GameStatistics seasonTotal = new GameStatistics();
			GameStatistics seasonOppTotal = new GameStatistics();
			GameStatistics tenTotal = new GameStatistics();
			GameStatistics tenOppTotal = new GameStatistics();
			int gamesPlayed = 0;
			for (int i = 0; i < homeGames.size(); i++) {
				Game g = homeGames.get(i);
				seasonTotal.addStats(g, true);
				seasonOppTotal.addStats(g, false);
				tenTotal.addStats(g, true);
				tenOppTotal.addStats(g, false);
				gamesPlayed++;
				if (gamesPlayed > 10) {
					Game pg = homeGames.get(i-10);
					tenTotal.subtractStats(pg, true);
					tenOppTotal.subtractStats(pg, false);
				}
				g.setSeasonHomeAvg(seasonTotal.calcAverage(gamesPlayed));
				g.setSeasonHomeOppAvg(seasonOppTotal.calcAverage(gamesPlayed));
				if (gamesPlayed >= EARLY_SEASON) {
					int tenPlayed = Math.min(gamesPlayed, 10);
					g.setTenHomeAvg(tenTotal.calcAverage(tenPlayed));
					g.setTenHomeOppAvg(tenOppTotal.calcAverage(tenPlayed));
				}
			}
		}

		public void calcRoadStats() {
			GameStatistics seasonTotal = new GameStatistics();
			GameStatistics seasonOppTotal = new GameStatistics();
			GameStatistics tenTotal = new GameStatistics();
			GameStatistics tenOppTotal = new GameStatistics();
			int gamesPlayed = 0;
			for (int i = 0; i < roadGames.size(); i++) {
				Game g = roadGames.get(i);
				seasonTotal.addStats(g, false);
				seasonOppTotal.addStats(g, true);
				tenTotal.addStats(g, false);
				tenOppTotal.addStats(g, true);
				gamesPlayed++;
				if (gamesPlayed > 10) {
					Game pg = roadGames.get(i-10);
					tenTotal.subtractStats(pg, false);
					tenOppTotal.subtractStats(pg, true);
				}
				g.setSeasonRoadAvg(seasonTotal.calcAverage(gamesPlayed));
				g.setSeasonRoadOppAvg(seasonOppTotal.calcAverage(gamesPlayed));
				if (gamesPlayed >= EARLY_SEASON) {
					int tenPlayed = Math.min(gamesPlayed, 10);
					g.setTenRoadAvg(tenTotal.calcAverage(tenPlayed));
					g.setTenRoadOppAvg(tenOppTotal.calcAverage(tenPlayed));
				}
			}
		}

	}

	public static void main(String args[]) {
		LogHelper.setDebug(true);
		for (int season=2007; season < 2013; season++) {
			readGameFile("C:/work/workspace/NBAStatFetch/data/" + season + ".csv");
			if (getTeamCount() > 0) {
				writeGameStats("C:/work/workspace/NBAStatFetch/data/SEASON-" + season + ".csv", true);
				writeGameStats("C:/work/workspace/NBAStatFetch/data/TEN-" + season + ".csv", false);
			}
		}
	}
}
