/**
 * NBAStatCalculator.java
 * From individual game statistics, calculates season averages (of games that 
 * came before current game)
 * 
 * @author Michelle Shu
 * Last Updated March 1, 2013
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;


public class NBAStatCalculator {
	static HashMap<String, Team> teams;
	static final int EARLY_SEASON = 5;	// Number of early season games to
										// exclude from averages.

	static int getTeamCount() {
		return teams.size();
	}

	/**
	 * Read NBA game records of a specified season, 
	 * and store the game in both home-team and road-team records.
	 */
	static void readGameFile(String gameFile) {
		BufferedReader reader = null;
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
		try {
			teams = new HashMap<String, Team>();
			reader = new BufferedReader(new FileReader(gameFile));
			String line;
			
			// If line is a header, store the labels of the header.
			boolean header = true;
			HashMap<String, Integer> homeHeader = new HashMap<String, Integer>();
			HashMap<String, Integer> roadHeader = new HashMap<String, Integer>();
			while ((line = reader.readLine()) != null) {
				String[] tokens = line.split(",");
				if (tokens.length >= 34) {
					if (header) { 
						header = false;
						for (int i = 3; i < 18; i++) {
							homeHeader.put(tokens[i], i);
							roadHeader.put(tokens[i + 16], i + 16);
						}
						continue;
					}
					try {
						// Create a record of the game.
						Date dt = fmt.parse(tokens[1]);
						Game game = new Game(dt);
						game.updateGame(homeHeader, tokens, true);
						game.updateGame(roadHeader, tokens, false);

						// Add the game to the road team's record.
						String roadTeamName = game.getRoadTeam();
						Team roadTeam = teams.get(roadTeamName);
						if (null == roadTeam) {
							roadTeam = new Team(roadTeamName);
							teams.put(roadTeamName, roadTeam);
						}
						roadTeam.addRoadGame(game);

						// Add the game to the home team's record.
						String homeTeamName = game.getHomeTeam();
						Team homeTeam = teams.get(homeTeamName);
						if (null == homeTeam) {
							homeTeam = new Team(homeTeamName);
							teams.put(homeTeamName, homeTeam);
						}
						homeTeam.addHomeGame(game);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
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

	/**
	 * Write the season stats (averages of previous games in season) to file.
	 */
	static void writeGameStats(String statFile, boolean useLastTen) {
		// Calculate statistics averages for each team's home and road games.
		for (Team team : teams.values()) {
			team.calcHomeStats();
			team.calcRoadStats();
		}

		// Write out the statistics by iterating through home games.
		PrintWriter out = null;
		try {
			out = new PrintWriter(new File(statFile));
			for (Team team : teams.values()) {
				for (Game g : team.getHomeGames()) {
					StringBuffer buff = new StringBuffer();
					
					/* Represent each game as a line of comma-separated values.
					 * Add the major feature statistics of team history for:
					 * 1. Home team history
					 * 2. Home team opponent history
					 * 3. Road team history
					 * 4. Road team opponent history
					 */
					buff.append(g.seasonHomeAvg.getMajorTeamStats()).append(",");
					buff.append(g.seasonHomeOppAvg.getMajorTeamStats()).append(",");
					buff.append(g.seasonRoadAvg.getMajorTeamStats()).append(",");
					buff.append(g.seasonRoadOppAvg.getMajorTeamStats()).append(",");
					
					/* Then add the two betting features: The difference in
					 * scores for the two teams for Against the Spread and the
					 * sum of the two team's scores for Over/Under.
					 */
					buff.append(g.homeStats.getTotalScore() - 
							g.roadStats.getTotalScore()).append(",");
					buff.append(g.homeStats.getTotalScore() + 
							g.roadStats.getTotalScore());
					out.println(buff.toString());
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

	/**
	 * Team:
	 * This inner class compartmentalizes the game history (home and road games)
	 * of a team.
	 */
	static public class Team {
		String name;
		ArrayList<Game> homeGames;
		ArrayList<Game> roadGames;

		/** Constructor */
		public Team(String name) {
			this.name = name;
			homeGames = new ArrayList<Game>();
			roadGames = new ArrayList<Game>();
		}

		/** Getters and Setters */
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

		/** Calculate the average past home game statistics of this team for 
		 *  the season relative to time of current game. */
		public void calcHomeStats() {
			GameStatistics seasonTotal = new GameStatistics();
			GameStatistics seasonOppTotal = new GameStatistics();

			int gamesPlayed = 0;
			for (int i = 0; i < homeGames.size()-1; i++) {
				Game g = homeGames.get(i);
				seasonTotal.addStats(g, true);
				seasonOppTotal.addStats(g, false);
				gamesPlayed++;

				Game nextGame = homeGames.get(i+1);
				nextGame.setSeasonHomeAvg(seasonTotal.calcAverage(gamesPlayed));
				nextGame.setSeasonHomeOppAvg(seasonOppTotal.calcAverage(gamesPlayed));
			}
		}

		/** Calculate the average past road game statistics of this team for 
		 *  the season relative to time of current game. */
		public void calcRoadStats() {
			GameStatistics seasonTotal = new GameStatistics();
			GameStatistics seasonOppTotal = new GameStatistics();
			int gamesPlayed = 0;
			for (int i = 0; i < roadGames.size()-1; i++) {
				Game g = roadGames.get(i);
				seasonTotal.addStats(g, false);
				seasonOppTotal.addStats(g, true);
				gamesPlayed++;

				Game nextGame = roadGames.get(i+1);
				nextGame.setSeasonRoadAvg(seasonTotal.calcAverage(gamesPlayed));
				nextGame.setSeasonRoadOppAvg(seasonOppTotal.calcAverage(gamesPlayed));
			}
		}

	}

	public static void main(String args[]) {
		for (int season=2007; season < 2013; season++) {
			readGameFile("data/" + season + ".csv");
			if (getTeamCount() > 0) {
				writeGameStats("data/SEASON-" + season + ".csv", true);
				writeGameStats("C:/work/workspace/NBAStatFetch/data/TEN-" + season + ".csv", false);
			}
		}
	}
}