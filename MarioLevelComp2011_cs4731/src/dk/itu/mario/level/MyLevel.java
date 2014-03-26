package dk.itu.mario.level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

import dk.itu.mario.MarioInterface.Constraints;
import dk.itu.mario.MarioInterface.GamePlay;
import dk.itu.mario.MarioInterface.LevelInterface;
import dk.itu.mario.engine.sprites.SpriteTemplate;
import dk.itu.mario.engine.sprites.Enemy;


public class MyLevel extends Level{
	//Store information about the level
	public   int ENEMIES = 0; //the number of enemies the level contains
	public   int BLOCKS_EMPTY = 0; // the number of empty blocks
	public   int BLOCKS_COINS = 0; // the number of coin blocks
	public   int BLOCKS_POWER = 0; // the number of power blocks
	public   int COINS = 0; //These are the coins in boxes that Mario collect
	public   int CANNONS = 0; //the number of cannons


	private static Random levelSeedRandom = new Random();
	public static long lastSeed;

	Random random;


	private int difficulty;
	private int type;
	private int gaps;
	
	//new vars
//	public static final int RUNNER = 1;
	public static final int PRECISION = 0;
	public static final int COLLECTOR = 1;
	public static final int KILLER = 2;
	public static final int HARDCORE = 3;
	
	public static final int JUMP_SECTION = 1;
	public static final int STRAIGHT_SECTION = 2;
	public static final int HILL_STRAIGHT_SECTION = 3;
	public static final int TUBES_SECTION = 4;
	public static final int CANNONS_SECTION = 5;
	
	private LevelSectionList levelSections;
	private int model;
	//end new vars

	public MyLevel(int width, int height)
	{
		super(width, height);
	}


	public MyLevel(int width, int height, long seed, int difficulty, int type, GamePlay playerMetrics)
	{
		this(width, height);
//		System.out.println("Aimless jumps" + playerMetrics.aimlessJumps);
		// hardcoded for now
		//model = decideModel();
		model = PRECISION;
		
		random = new Random(seed);
		
		levelSections = generateSections(width);
		
		creat(seed, difficulty, type);
//		System.out.println(evaluate(model));
	}
	
//	//new methods
	public int decideModel() {
		GamePlay stats = GamePlay.read("player.txt");
		double[] vals = {0, 0, 0, 0};
		//precision
		vals[0] = 1/(stats.aimlessJumps + 1);
		//collector
		vals[1] = ((double) stats.coinsCollected + 1)/(stats.totalCoins + 1);
		//killer
		vals[2] = ((double) stats.RedTurtlesKilled + stats.GreenTurtlesKilled + stats.ArmoredTurtlesKilled + stats.GoombasKilled + stats.CannonBallKilled + stats.JumpFlowersKilled + stats.ChompFlowersKilled + 1)/(stats.totalEnemies + 1);
		//hardcore
		vals[3] = (vals[0] + vals[1] + vals[2])/2;
		
		double temp = 0;
		int ret = 0;
		for (int i = 0; i < vals.length; i++) {
			System.out.println(vals[i]);
			if (vals[i] > temp) {
				temp = vals[i];
				ret = i;
			}
		}
		System.out.println(ret);
		return ret;
	}
//	//end new methods

	public void creat(long seed, int difficulty, int type)
	{
		this.type = type;
		this.difficulty = difficulty;

		lastSeed = seed;

		//create the start location
		int length = 0;
		length += buildStraight(0, width, true);
		
		for(LevelSection ls : levelSections)
		{
			length += ls.build(length, width - length);
		}

		//create all of the medium sections
//		while (length < width - 64)
//		{
//			//length += buildZone(length, width - length);
//			length += buildStraight(length, width-length, false);
//			length += buildStraight(length, width-length, false);
//			length += buildHillStraight(length, width-length);
//			length += buildJump(length, width-length);
//			length += buildTubes(length, width-length);
//			length += buildCannons(length, width-length);
//			
//			//test
//			length += buildAdvancedJump(length, width-length);
//		}

		//set the end piece
		int floor = height - 1 - random.nextInt(4);

		xExit = length + 8;
		yExit = floor;

		// fills the end piece
		for (int x = length; x < width; x++)
		{
			for (int y = 0; y < height; y++)
			{
				if (y >= floor)
				{
					setBlock(x, y, GROUND);
				}
			}
		}

		if (type == LevelInterface.TYPE_CASTLE || type == LevelInterface.TYPE_UNDERGROUND)
		{
			int ceiling = 0;
			int run = 0;
			for (int x = 0; x < width; x++)
			{
				if (run-- <= 0 && x > 4)
				{
					ceiling = random.nextInt(4);
					run = random.nextInt(4) + 4;
				}
				for (int y = 0; y < height; y++)
				{
					if ((x > 4 && y <= ceiling) || x < 1)
					{
						setBlock(x, y, GROUND);
					}
				}
			}
		}

		fixWalls();

	}


	private int buildJump(int xo, int maxLength)
	{	gaps++;
	//jl: jump length
	//js: the number of blocks that are available at either side for free
	int js = random.nextInt(4) + 2;
	int jl = random.nextInt(2) + 2;
	int length = js * 2 + jl;

	boolean hasStairs = random.nextInt(3) == 0;

	int floor = height - 1 - random.nextInt(4);
	//run from the start x position, for the whole length
	for (int x = xo; x < xo + length; x++)
	{
		if (x < xo + js || x > xo + length - js - 1)
		{
			//run for all y's since we need to paint blocks upward
			for (int y = 0; y < height; y++)
			{	//paint ground up until the floor
				if (y >= floor)
				{
					setBlock(x, y, GROUND);
				}
				//if it is above ground, start making stairs of rocks
				else if (hasStairs)
				{	//LEFT SIDE
					if (x < xo + js)
					{ //we need to max it out and level because it wont
						//paint ground correctly unless two bricks are side by side
						if (y >= floor - (x - xo) + 1)
						{
							setBlock(x, y, ROCK);
						}
					}
					else
					{ //RIGHT SIDE
						if (y >= floor - ((xo + length) - x) + 2)
						{
							setBlock(x, y, ROCK);
						}
					}
				}
			}
		}
	}

	return length;
	}

	private int buildCannons(int xo, int maxLength)
	{
		int length = random.nextInt(10) + 2;
		if (length > maxLength) length = maxLength;

		int floor = height - 1 - random.nextInt(4);
		int xCannon = xo + 1 + random.nextInt(4);
		for (int x = xo; x < xo + length; x++)
		{
			if (x > xCannon)
			{
				xCannon += 2 + random.nextInt(4);
			}
			if (xCannon == xo + length - 1) xCannon += 10;
			int cannonHeight = floor - random.nextInt(4) - 1;

			for (int y = 0; y < height; y++)
			{
				if (y >= floor)
				{
					setBlock(x, y, GROUND);
				}
				else
				{
					if (x == xCannon && y >= cannonHeight)
					{
						if (y == cannonHeight)
						{
							setBlock(x, y, (byte) (14 + 0 * 16));
						}
						else if (y == cannonHeight + 1)
						{
							setBlock(x, y, (byte) (14 + 1 * 16));
						}
						else
						{
							setBlock(x, y, (byte) (14 + 2 * 16));
						}
					}
				}
			}
		}

		return length;
	}

	private int buildHillStraight(int xo, int maxLength)
	{
		int length = random.nextInt(10) + 10;
		if (length > maxLength) length = maxLength;

		int floor = height - 1 - random.nextInt(4);
		for (int x = xo; x < xo + length; x++)
		{
			for (int y = 0; y < height; y++)
			{
				if (y >= floor)
				{
					setBlock(x, y, GROUND);
				}
			}
		}

		addEnemyLine(xo + 1, xo + length - 1, floor - 1);

		int h = floor;

		boolean keepGoing = true;

		boolean[] occupied = new boolean[length];
		while (keepGoing)
		{
			h = h - 2 - random.nextInt(3);

			if (h <= 0)
			{
				keepGoing = false;
			}
			else
			{
				int l = random.nextInt(5) + 3;
				int xxo = random.nextInt(length - l - 2) + xo + 1;

				if (occupied[xxo - xo] || occupied[xxo - xo + l] || occupied[xxo - xo - 1] || occupied[xxo - xo + l + 1])
				{
					keepGoing = false;
				}
				else
				{
					occupied[xxo - xo] = true;
					occupied[xxo - xo + l] = true;
					addEnemyLine(xxo, xxo + l, h - 1);
					if (random.nextInt(4) == 0)
					{
						decorate(xxo - 1, xxo + l + 1, h);
						keepGoing = false;
					}
					for (int x = xxo; x < xxo + l; x++)
					{
						for (int y = h; y < floor; y++)
						{
							int xx = 5;
							if (x == xxo) xx = 4;
							if (x == xxo + l - 1) xx = 6;
							int yy = 9;
							if (y == h) yy = 8;

							if (getBlock(x, y) == 0)
							{
								setBlock(x, y, (byte) (xx + yy * 16));
							}
							else
							{
								if (getBlock(x, y) == HILL_TOP_LEFT) setBlock(x, y, HILL_TOP_LEFT_IN);
								if (getBlock(x, y) == HILL_TOP_RIGHT) setBlock(x, y, HILL_TOP_RIGHT_IN);
							}
						}
					}
				}
			}
		}

		return length;
	}

	private void addEnemyLine(int x0, int x1, int y)
	{
		for (int x = x0; x < x1; x++)
		{
			if (random.nextInt(35) < difficulty + 1)
			{
				int type = random.nextInt(4);

				if (difficulty < 1)
				{
					type = Enemy.ENEMY_GOOMBA;
				}
				else if (difficulty < 3)
				{
					type = random.nextInt(3);
				}

				setSpriteTemplate(x, y, new SpriteTemplate(type, random.nextInt(35) < difficulty));
				ENEMIES++;
			}
		}
	}

	private int buildTubes(int xo, int maxLength)
	{
		int length = random.nextInt(10) + 5;
		if (length > maxLength) length = maxLength;

		int floor = height - 1 - random.nextInt(4);
		int xTube = xo + 1 + random.nextInt(4);
		int tubeHeight = floor - random.nextInt(2) - 2;
		for (int x = xo; x < xo + length; x++)
		{
			if (x > xTube + 1)
			{
				xTube += 3 + random.nextInt(4);
				tubeHeight = floor - random.nextInt(2) - 2;
			}
			if (xTube >= xo + length - 2) xTube += 10;

			if (x == xTube && random.nextInt(11) < difficulty + 1)
			{
				setSpriteTemplate(x, tubeHeight, new SpriteTemplate(Enemy.ENEMY_FLOWER, false));
				ENEMIES++;
			}

			for (int y = 0; y < height; y++)
			{
				if (y >= floor)
				{
					setBlock(x, y,GROUND);

				}
				else
				{
					if ((x == xTube || x == xTube + 1) && y >= tubeHeight)
					{
						int xPic = 10 + x - xTube;

						if (y == tubeHeight)
						{
							//tube top
							setBlock(x, y, (byte) (xPic + 0 * 16));
						}
						else
						{
							//tube side
							setBlock(x, y, (byte) (xPic + 1 * 16));
						}
					}
				}
			}
		}

		return length;
	}

	private int buildStraight(int xo, int maxLength, boolean safe)
	{
		int length = random.nextInt(10) + 2;

		if (safe)
			length = 10 + random.nextInt(5);

		if (length > maxLength)
			length = maxLength;

		int floor = height - 1 - random.nextInt(4);

		//runs from the specified x position to the length of the segment
		for (int x = xo; x < xo + length; x++)
		{
			for (int y = 0; y < height; y++)
			{
				if (y >= floor)
				{
					setBlock(x, y, GROUND);
				}
			}
		}

		if (!safe)
		{
			if (length > 5)
			{
				decorate(xo, xo + length, floor);
			}
		}

		return length;
	}

	private void decorate(int xStart, int xLength, int floor)
	{
		//if its at the very top, just return
				if (floor < 1)
					return;

		//        boolean coins = random.nextInt(3) == 0;
		boolean rocks = true;

		//add an enemy line above the box
		addEnemyLine(xStart + 1, xLength - 1, floor - 1);

		int s = random.nextInt(4);
		int e = random.nextInt(4);

		if (floor - 2 > 0){
			if ((xLength - 1 - e) - (xStart + 1 + s) > 1){
				for(int x = xStart + 1 + s; x < xLength - 1 - e; x++){
					setBlock(x, floor - 2, COIN);
					COINS++;
				}
			}
		}

		s = random.nextInt(4);
		e = random.nextInt(4);

		//this fills the set of blocks and the hidden objects inside them
		if (floor - 4 > 0)
		{
			if ((xLength - 1 - e) - (xStart + 1 + s) > 2)
			{
				for (int x = xStart + 1 + s; x < xLength - 1 - e; x++)
				{
					if (rocks)
					{
						if (x != xStart + 1 && x != xLength - 2 && random.nextInt(3) == 0)
						{
							if (random.nextInt(4) == 0)
							{
								setBlock(x, floor - 4, BLOCK_POWERUP);
								BLOCKS_POWER++;
							}
							else
							{	//the fills a block with a hidden coin
								setBlock(x, floor - 4, BLOCK_COIN);
							BLOCKS_COINS++;
							}
						}
						else if (random.nextInt(4) == 0)
						{
							if (random.nextInt(4) == 0)
							{
								setBlock(x, floor - 4, (byte) (2 + 1 * 16));
							}
							else
							{
								setBlock(x, floor - 4, (byte) (1 + 1 * 16));
							}
						}
						else
						{
							setBlock(x, floor - 4, BLOCK_EMPTY);
							BLOCKS_EMPTY++;
						}
					}
				}
			}
		}
	}

	private void fixWalls()
	{
		boolean[][] blockMap = new boolean[width + 1][height + 1];

		for (int x = 0; x < width + 1; x++)
		{
			for (int y = 0; y < height + 1; y++)
			{
				int blocks = 0;
				for (int xx = x - 1; xx < x + 1; xx++)
				{
					for (int yy = y - 1; yy < y + 1; yy++)
					{
						if (getBlockCapped(xx, yy) == GROUND){
							blocks++;
						}
					}
				}
				blockMap[x][y] = blocks == 4;
			}
		}
		blockify(this, blockMap, width + 1, height + 1);
	}

	private void blockify(Level level, boolean[][] blocks, int width, int height){
		int to = 0;
		if (type == LevelInterface.TYPE_CASTLE)
		{
			to = 4 * 2;
		}
		else if (type == LevelInterface.TYPE_UNDERGROUND)
		{
			to = 4 * 3;
		}

		boolean[][] b = new boolean[2][2];

		for (int x = 0; x < width; x++)
		{
			for (int y = 0; y < height; y++)
			{
				for (int xx = x; xx <= x + 1; xx++)
				{
					for (int yy = y; yy <= y + 1; yy++)
					{
						int _xx = xx;
						int _yy = yy;
						if (_xx < 0) _xx = 0;
						if (_yy < 0) _yy = 0;
						if (_xx > width - 1) _xx = width - 1;
						if (_yy > height - 1) _yy = height - 1;
						b[xx - x][yy - y] = blocks[_xx][_yy];
					}
				}

				if (b[0][0] == b[1][0] && b[0][1] == b[1][1])
				{
					if (b[0][0] == b[0][1])
					{
						if (b[0][0])
						{
							level.setBlock(x, y, (byte) (1 + 9 * 16 + to));
						}
						else
						{
							// KEEP OLD BLOCK!
						}
					}
					else
					{
						if (b[0][0])
						{
							//down grass top?
									level.setBlock(x, y, (byte) (1 + 10 * 16 + to));
						}
						else
						{
							//up grass top
							level.setBlock(x, y, (byte) (1 + 8 * 16 + to));
						}
					}
				}
				else if (b[0][0] == b[0][1] && b[1][0] == b[1][1])
				{
					if (b[0][0])
					{
						//right grass top
						level.setBlock(x, y, (byte) (2 + 9 * 16 + to));
					}
					else
					{
						//left grass top
						level.setBlock(x, y, (byte) (0 + 9 * 16 + to));
					}
				}
				else if (b[0][0] == b[1][1] && b[0][1] == b[1][0])
				{
					level.setBlock(x, y, (byte) (1 + 9 * 16 + to));
				}
				else if (b[0][0] == b[1][0])
				{
					if (b[0][0])
					{
						if (b[0][1])
						{
							level.setBlock(x, y, (byte) (3 + 10 * 16 + to));
						}
						else
						{
							level.setBlock(x, y, (byte) (3 + 11 * 16 + to));
						}
					}
					else
					{
						if (b[0][1])
						{
							//right up grass top
							level.setBlock(x, y, (byte) (2 + 8 * 16 + to));
						}
						else
						{
							//left up grass top
							level.setBlock(x, y, (byte) (0 + 8 * 16 + to));
						}
					}
				}
				else if (b[0][1] == b[1][1])
				{
					if (b[0][1])
					{
						if (b[0][0])
						{
							//left pocket grass
							level.setBlock(x, y, (byte) (3 + 9 * 16 + to));
						}
						else
						{
							//right pocket grass
							level.setBlock(x, y, (byte) (3 + 8 * 16 + to));
						}
					}
					else
					{
						if (b[0][0])
						{
							level.setBlock(x, y, (byte) (2 + 10 * 16 + to));
						}
						else
						{
							level.setBlock(x, y, (byte) (0 + 10 * 16 + to));
						}
					}
				}
				else
				{
					level.setBlock(x, y, (byte) (0 + 1 * 16 + to));
				}
			}
		}
	}

	public RandomLevel clone() throws CloneNotSupportedException {

		RandomLevel clone=new RandomLevel(width, height);

		clone.xExit = xExit;
		clone.yExit = yExit;
		byte[][] map = getMap();
		SpriteTemplate[][] st = getSpriteTemplate();

		for (int i = 0; i < map.length; i++)
			for (int j = 0; j < map[i].length; j++) {
				clone.setBlock(i, j, map[i][j]);
				clone.setSpriteTemplate(i, j, st[i][j]);
			}
		clone.BLOCKS_COINS = BLOCKS_COINS;
		clone.BLOCKS_EMPTY = BLOCKS_EMPTY;
		clone.BLOCKS_POWER = BLOCKS_POWER;
		clone.ENEMIES = ENEMIES;
		clone.COINS = COINS;

		return clone;

	}

	//new classes
//	public abstract class LevelSection {
//		public int id;
//		
//		public int enemies;
//		public int requiredJumps;
//		public int coins;
//		public int difficulty;
//		public int runnability;
//		public int powerups;
//		
//		public int length;
//		public int start;
//		public int end;
//	}
	
	public LevelSectionList generateSections(int width)
	{
		final int POPULATION_SIZE = 50;
		final int GENERATIONS = 50;
		final int NUMBER_TO_KEEP = 10;
		
		//Initial population
		ArrayList<LevelSectionList> population = new ArrayList<LevelSectionList>();
		
		for(int i=0; i < POPULATION_SIZE; i++)
		{
			LevelSectionList level = new LevelSectionList();
			while(level.length < width - 64)
			{
				LevelSection ls = randomSection(width-level.length);
				level.add(ls);
			}
			population.add(level);
		}
		
		for(int i=0; i < GENERATIONS; i++)
		{
			//Selection
			Collections.sort(population);
			System.out.println("Best level fitness = " + population.get(0).fitness);
			
			population.subList(NUMBER_TO_KEEP, POPULATION_SIZE).clear();
			
			//Mutation
			for(int j=0; j < POPULATION_SIZE - NUMBER_TO_KEEP; j++)
			{
				LevelSectionList parentA = population.get(random.nextInt(NUMBER_TO_KEEP));
				LevelSectionList parentB = population.get(random.nextInt(NUMBER_TO_KEEP));
				
				LevelSectionList level = new LevelSectionList();
				int k = 0;
				while(level.length < width - 64)
				{
					LevelSection ls;
					
					if(k < parentA.size() && k < parentB.size())
					{
						ls = random.nextBoolean() ? parentA.get(k) : parentB.get(k);
					}
					else if(k < parentA.size())
					{
						ls = parentA.get(k);
					}
					else if(k < parentB.size())
					{
						ls = parentB.get(k);
					}
					else
					{
						ls = randomSection(width-level.length);
					}
					
					level.add(ls);
					k++;
				}
				
				population.add(level);
			}
		}
		
		Collections.sort(population);
		
		return population.get(0);
	}
	
	public LevelSection randomSection(int maxLength)
	{
		int id = random.nextInt(3);
		//TODO add more sections options
		switch(id) {
		case 0:
			return new AdvancedJumpSection();
		case 1:
			return new StraightSection(maxLength);
		case 2:
			return new CannonSection(maxLength);
		case 3:
			return new TubeSection(maxLength);
		case 4:
			return new HillSection(maxLength);
		default:
			return null;
		}
	}
	
	public class LevelSectionList implements Comparable<LevelSectionList>, Iterable<LevelSection>{
		public int fitness;
		public int length;
		
		ArrayList<LevelSection> sections;
		
		public LevelSectionList()
		{
			sections = new ArrayList<LevelSection>();
			fitness = 0;
			length = 0;
		}
		
		public void add(LevelSection ls)
		{
			sections.add(ls);
			length += ls.getLength();
			fitness += ls.fitness();
		}
		
		public LevelSection get(int i)
		{
			return sections.get(i);
		}
		
		public int size()
		{
			return sections.size();
		}

		@Override
		public int compareTo(LevelSectionList o) {
										//PRECISION, COLLECTOR, KILLER, HARDCORE
			final double[] MODEL_TARGETS = {100, 300, 100, 100};
			
			double target = MODEL_TARGETS[model];
			if(Math.abs(o.fitness - target) < Math.abs(this.fitness - target)) return 1;
			else if(Math.abs(this.fitness - target) < Math.abs(o.fitness - target)) return -1;
			else return 0;
		}

		@Override
		public Iterator<LevelSection> iterator() {
			return sections.iterator();
		}
	}
	
	public abstract class LevelSection {
		public abstract int getId();
		public abstract int getLength();
		public abstract double fitness();
		public abstract int build(int xo, int maxLength);
	}
	
	//jump
	public class AdvancedJumpSection extends LevelSection{
		
		//jl: jump length
		//js: the number of blocks that are available at either side for free
		//vDiff: vertical difference across the gap
		//hasStairs: a block in front of the jump makes it more difficult
		private int id = JUMP_SECTION;
		private int vDiff;
		private int jl;
		private int js;
		private boolean hasStairs;
		
		private int length;
		
		public AdvancedJumpSection()
		{
			boolean valid = false;
			while(!valid)
			{
				js = random.nextInt(2) + 2;
				jl = random.nextInt(6) + 1;
				vDiff = random.nextInt(9) - 4;
				hasStairs = random.nextInt(3) == 0;
				
				if(jl - vDiff > 8) valid = false;
				else valid = true;
			}
			
			length = js * 2 + jl;
		}
		
		public int getId() {
			return id;
		}
		
		public int getLength()
		{
			return length;
		}
		
		public double fitness()
		{
			switch(model)
			{
			case PRECISION:
				return jl - js - vDiff + (hasStairs ? 3 : 0);
				
			case COLLECTOR:
				return 0;
				
			case KILLER:
				return 0;
				
			case HARDCORE:
				return jl - js - vDiff + (hasStairs ? 3 : 0);
				
			default:
				
				break;
			}
			return 0.0;
		}
		
		public int build(int xo, int maxLength) {
			gaps++;
		
			int floor = height - 1 - random.nextInt(5);

			//run from the start x position, for the whole length
			for (int x = xo; x < xo + length; x++)
			{
				if (x == xo + js) {
					floor = floor + vDiff;
					if (floor < 0) {
						floor = 0;
					} else if (floor > height - 1) {
						floor = height - 1;
					}
				}
				if (x < xo + js || x > xo + length - js - 1)
				{
					//run for all y's since we need to paint blocks upward
					for (int y = 0; y < height; y++)
					{	//paint ground up until the floor
						if (y >= floor)
						{
							setBlock(x, y, GROUND);
						}
						//if it is above ground, start making stairs of rocks
						else if (hasStairs)
						{	//LEFT SIDE
							if (x < xo + js)
							{ //we need to max it out and level because it wont
								//paint ground correctly unless two bricks are side by side
								if (y >= floor - (x - xo) + 1)
								{
									setBlock(x, y, ROCK);
								}
							}
							else
							{ //RIGHT SIDE
								if (y >= floor - ((xo + length) - x) + 2)
								{
									setBlock(x, y, ROCK);
								}
							}
						}
					}
				}
			}
			
			return length;
		}
	}
	
	//straight
	public class StraightSection extends LevelSection{
		
		private int id = STRAIGHT_SECTION;
		
		private int length;
		private int enemies;
		private int coins;
		
		public StraightSection(int maxLength)
		{
			length = random.nextInt(10) + 2;
			
			enemies = random.nextInt(4);
			//only 1/4 chance of being 3 or more enemies
			if(enemies == 3) enemies = 3 + random.nextInt(3);
			
			if(random.nextBoolean())
			{
				coins = random.nextInt(6) + 1;
			}
			else coins = 0;

			if (length > maxLength)
				length = maxLength;
		}
		
		public int getId() {
			return id;
		}
		
		public int getLength()
		{
			return length;
		}
		
		public double fitness()
		{
			switch(model)
			{
			case PRECISION:
				return -length + 2;
				
			case COLLECTOR:
				return coins - (enemies/2);
				
			case KILLER:
				return enemies - (coins/2);
				
			case HARDCORE:
				return coins + enemies;
			
			default:
				return 0.0;
			}
		}
		
		public int build(int xo, int maxLength) {
			int floor = height - 1 - random.nextInt(4);

			//runs from the specified x position to the length of the segment
			for (int x = xo; x < xo + length; x++)
			{
				for (int y = 0; y < height; y++)
				{
					if (y >= floor)
					{
						setBlock(x, y, GROUND);
					}
				}
			}

			if (length > 5)
			{
				if(enemies > 0) addEnemies(xo, xo+length, floor, enemies);
				if(coins > 0) addCoins(xo, xo+length, floor, coins);
			}
			
			return length;
		}
	}
	
	//hill
	public class HillSection extends LevelSection{
		
		private int id = HILL_STRAIGHT_SECTION;
		
		private int length;
		private int enemies;
		private int coins;
		
		public HillSection(int maxLength)
		{
			length = random.nextInt(10) + 10;
			
			enemies = random.nextInt(4);
			//only 1/4 chance of being 3 or more enemies
			if(enemies == 3) enemies = 3 + random.nextInt(3);
			
			if(random.nextBoolean())
			{
				coins = random.nextInt(6) + 1;
			}
			else coins = 0;
			
			if (length > maxLength) length = maxLength;
		}
		
		public int getId() {
			return id;
		}
		
		public int getLength()
		{
			return length;
		}
		
		public double fitness()
		{
			switch(model)
			{
			case PRECISION:
				return -length + 2;
				
			case COLLECTOR:
				return coins;
				
			case KILLER:
				return enemies;
				
			case HARDCORE:
				return coins + enemies;
			
			default:
				return 0.0;
			}
		}
		
		public int build(int xo, int maxLength) {
			int floor = height - 1 - random.nextInt(4);
			for (int x = xo; x < xo + length; x++)
			{
				for (int y = 0; y < height; y++)
				{
					if (y >= floor)
					{
						setBlock(x, y, GROUND);
					}
				}
			}

			addEnemyLine(xo + 1, xo + length - 1, floor - 1);

			int h = floor;

			boolean keepGoing = true;

			boolean[] occupied = new boolean[length];
			while (keepGoing)
			{
				h = h - 2 - random.nextInt(3);

				if (h <= 0)
				{
					keepGoing = false;
				}
				else
				{
					int l = random.nextInt(5) + 3;
					int xxo = random.nextInt(length - l - 2) + xo + 1;

					if (occupied[xxo - xo] || occupied[xxo - xo + l] || occupied[xxo - xo - 1] || occupied[xxo - xo + l + 1])
					{
						keepGoing = false;
					}
					else
					{
						occupied[xxo - xo] = true;
						occupied[xxo - xo + l] = true;
						addEnemyLine(xxo, xxo + l, h - 1);
						if (random.nextInt(4) == 0)
						{
							decorate(xxo - 1, xxo + l + 1, h);
							keepGoing = false;
						}
						for (int x = xxo; x < xxo + l; x++)
						{
							for (int y = h; y < floor; y++)
							{
								int xx = 5;
								if (x == xxo) xx = 4;
								if (x == xxo + l - 1) xx = 6;
								int yy = 9;
								if (y == h) yy = 8;

								if (getBlock(x, y) == 0)
								{
									setBlock(x, y, (byte) (xx + yy * 16));
								}
								else
								{
									if (getBlock(x, y) == HILL_TOP_LEFT) setBlock(x, y, HILL_TOP_LEFT_IN);
									if (getBlock(x, y) == HILL_TOP_RIGHT) setBlock(x, y, HILL_TOP_RIGHT_IN);
								}
							}
						}
					}
				}
			}

			return length;
		}
	}
	
	//tubes
	public class TubeSection extends LevelSection{
		
		private int id = TUBES_SECTION;
		
		private int length;
		
		public TubeSection(int maxLength)
		{
			length = random.nextInt(10) + 5;
			if (length > maxLength) length = maxLength;
		}
		
		public int getId() {
			return id;
		}
		
		public int getLength()
		{
			return length;
		}
		
		public double fitness()
		{
			switch(model)
			{
			case PRECISION:
				return -length + 2;
			default:
				
				break;
			}
			return 0.0;
		}
		
		public int build(int xo, int maxLength) {
			int floor = height - 1 - random.nextInt(4);
			int xTube = xo + 1 + random.nextInt(4);
			int tubeHeight = floor - random.nextInt(2) - 2;
			for (int x = xo; x < xo + length; x++)
			{
				if (x > xTube + 1)
				{
					xTube += 3 + random.nextInt(4);
					tubeHeight = floor - random.nextInt(2) - 2;
				}
				if (xTube >= xo + length - 2) xTube += 10;

				if (x == xTube && random.nextInt(11) < difficulty + 1)
				{
					setSpriteTemplate(x, tubeHeight, new SpriteTemplate(Enemy.ENEMY_FLOWER, false));
					ENEMIES++;
				}

				for (int y = 0; y < height; y++)
				{
					if (y >= floor)
					{
						setBlock(x, y,GROUND);

					}
					else
					{
						if ((x == xTube || x == xTube + 1) && y >= tubeHeight)
						{
							int xPic = 10 + x - xTube;

							if (y == tubeHeight)
							{
								//tube top
								setBlock(x, y, (byte) (xPic + 0 * 16));
							}
							else
							{
								//tube side
								setBlock(x, y, (byte) (xPic + 1 * 16));
							}
						}
					}
				}
			}
			
			return length;
		}
	}
	
	//cannons
	public class CannonSection extends LevelSection{
		
		private int id = CANNONS_SECTION;
		
		private int length;
		private boolean[] cannonLocations;
		private int cannons;
		
		public CannonSection(int maxLength)
		{
			length = random.nextInt(10) + 2;
			if (length > maxLength) length = maxLength;
			
			cannonLocations = new boolean[length];
			for (int i = 0; i < length; i++) {
				if (random.nextInt(4) == 0) { // 1/4 chance of cannons
					cannonLocations[i] = true;
					cannons++;
				}
			}
		}
		
		public int getId() {
			return id;
		}
		
		public int getLength()
		{
			return length;
		}
		
		public double fitness()
		{
			switch(model)
			{
			case PRECISION:
				return -length + 2;
			default:
				
				break;
			}
			return 0.0;
		}
		
		public int build(int xo, int maxLength) {
			int floor = height - 1 - random.nextInt(4);
			for (int i = 0; i < length; i++)
			{
				int cannonHeight = floor - random.nextInt(4) - 1;

				for (int y = 0; y < height; y++)
				{
					if (y >= floor)
					{
						setBlock(xo + i, y, GROUND);
					}
					else if (y >= cannonHeight && cannonLocations[i])
					{
						if (y == cannonHeight)
						{
							setBlock(xo + i, y, (byte) (14 + 0 * 16));
						}
						else if (y == cannonHeight + 1)
						{
							setBlock(xo + i, y, (byte) (14 + 1 * 16));
						}
						else
						{
							setBlock(xo + i, y, (byte) (14 + 2 * 16));
						}
					}
				}
			}

			return length;
		}
	}
	
	private void addEnemies(int startX, int endX, int y, int number)
	{
		if (y - 1 <= 0) return;
		
		for (int i=0; i < number; i++)
		{
			int x = random.nextInt(endX - startX) + startX;
			while(getSpriteTemplate(x, y-1) != null)
			{
				x = random.nextInt(endX - startX) + startX;
			}
				
			int type = random.nextInt(4);
			boolean winged = random.nextInt(30) < 2;

			setSpriteTemplate(x, y-1, new SpriteTemplate(type, winged));
			ENEMIES++;
		}
	}
	
	private void addCoins(int startX, int endX, int y, int number)
	{
		if (y - 2 <= 0) return;
		
		for (int i=0; i < number; i++)
		{
			int x = random.nextInt(endX - startX) + startX;
			while(this.getBlock(x, y - 2) == COIN)
			{
				x = random.nextInt(endX - startX) + startX;
			}
				
			setBlock(x, y - 2, COIN);
			COINS++;
		}
	}
	
//	public class PrecisionSection extends LevelSection {
//		public int difficulty;
//	}
	//end new classes
}
