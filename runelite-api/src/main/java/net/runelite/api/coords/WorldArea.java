/*
 * Copyright (c) 2018, Woox <https://github.com/wooxsolo>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.api.coords;

import java.util.function.Predicate;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.Constants;
import net.runelite.api.Point;
import net.runelite.api.Tile;

public class WorldArea
{
	/**
	 * The western most point of the area
	 */
	@Getter
	private int x;

	/**
	 * The southern most point of the area
	 */
	@Getter
	private int y;

	/**
	 * The width of the area
	 */
	@Getter
	private int width;

	/**
	 * The height of the area
	 */
	@Getter
	private int height;

	/**
	 * The plane the area is on
	 */
	@Getter
	private int plane;

	public WorldArea(int x, int y, int width, int height, int plane)
	{
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.plane = plane;
	}

	public WorldArea(WorldPoint location, int width, int height)
	{
		this.x = location.getX();
		this.y = location.getY();
		this.plane = location.getPlane();
		this.width = width;
		this.height = height;
	}

	/**
	 * Get the shortest distance to another WorldArea for both x and y axis
	 * @param other The WorldArea to get the distance to
	 * @return Returns a Point with the shortest distance
	 */
	private Point getAxisDistances(WorldArea other)
	{
		Point p1 = this.getComparisonPoint(other);
		Point p2 = other.getComparisonPoint(this);
		return new Point(Math.abs(p1.getX() - p2.getX()), Math.abs(p1.getY() - p2.getY()));
	}

	/**
	 * Get the shortest distance to another WorldArea
	 *
	 * @param other The other area
	 * @return Returns the distance
	 */
	public int distanceTo(WorldArea other)
	{
		if (this.getPlane() != other.getPlane())
		{
			return Integer.MAX_VALUE;
		}

		Point distances = getAxisDistances(other);
		return Math.max(distances.getX(), distances.getY());
	}

	/**
	 * Get the shortest distance to another WorldPoint
	 *
	 * @param other The other worldpoint
	 * @return Returns the distance
	 */
	public int distanceTo(WorldPoint other)
	{
		return distanceTo(new WorldArea(other, 1, 1));
	}

	/**
	 * Determines if this WorldArea is within melee distance of another WorldArea
	 *
	 * @param other The other world area to compare with
	 * @return Returns true if it is in melee distance
	 */
	public boolean isInMeleeDistance(WorldArea other)
	{
		if (other == null || this.getPlane() != other.getPlane())
		{
			return false;
		}

		Point distances = getAxisDistances(other);
		return distances.getX() + distances.getY() == 1;
	}

	/**
	 * Determines if this WorldArea is within melee distance of another WorldPoint
	 *
	 * @param other The world pint to compare with
	 * @return Returns true if it is in melee distance
	 */
	public boolean isInMeleeDistance(WorldPoint other)
	{
		return isInMeleeDistance(new WorldArea(other, 1, 1));
	}

	/**
	 * Determines if a WorldArea intersects with another WorldArea
	 *
	 * @param other The other WorldArea to compare with
	 * @return Returns true if the areas intersect
	 */
	public boolean intersectsWith(WorldArea other)
	{
		if (this.getPlane() != other.getPlane())
		{
			return false;
		}

		Point distances = getAxisDistances(other);
		return distances.getX() + distances.getY() == 0;
	}

	/**
	 * Determines if the area can travel in one of the 8 directions
	 * by using the standard collision detection algorithm.
	 * Note that this method does not consider other actors as
	 * a collision, but most non-boss NPCs do check for collision
	 * with some actors.
	 *
	 * @param client The client to test in
	 * @param dx The x direction to test against
	 * @param dy The y direction to test against
	 * @return Returns true if it's possible to travel in specified direction
	 */
	public boolean canTravelInDirection(Client client, int dx, int dy)
	{
		return canTravelInDirection(client, dx, dy, x -> true);
	}

	/**
	 * Determines if the area can travel in one of the 8 directions
	 * by using the standard collision detection algorithm.
	 * Note that this method does not consider other actors as
	 * a collision, but most non-boss NPCs do check for collision
	 * with some actors.
	 *
	 * @param client The client to test in
	 * @param dx The x direction to test against
	 * @param dy The y direction to test against
	 * @param extraCondition Additional check for if movement is allowed through specific
	 * tiles, which may be used if movement should be disabled through other actors
	 * @return Returns true if it's possible to travel in specified direction
	 */
	public boolean canTravelInDirection(Client client, int dx, int dy,
										Predicate<? super WorldPoint> extraCondition)
	{
		dx = Integer.signum(dx);
		dy = Integer.signum(dy);

		if (dx == 0 && dy == 0)
		{
			return true;
		}

		LocalPoint lp = LocalPoint.fromWorld(client, x, y);

		int startX = lp.getRegionX() + dx;
		int startY = lp.getRegionY() + dy;
		int checkX = startX + (dx > 0 ? width - 1 : 0);
		int checkY = startY + (dy > 0 ? height - 1 : 0);
		int endX = startX + width - 1;
		int endY = startY + height - 1;

		int xFlags = CollisionDataFlag.BLOCK_MOVEMENT_FULL;
		int yFlags = CollisionDataFlag.BLOCK_MOVEMENT_FULL;
		int xyFlags = CollisionDataFlag.BLOCK_MOVEMENT_FULL;
		int xWallFlagsSouth = CollisionDataFlag.BLOCK_MOVEMENT_FULL;
		int xWallFlagsNorth = CollisionDataFlag.BLOCK_MOVEMENT_FULL;
		int yWallFlagsWest = CollisionDataFlag.BLOCK_MOVEMENT_FULL;
		int yWallFlagsEast = CollisionDataFlag.BLOCK_MOVEMENT_FULL;

		if (dx < 0)
		{
			xFlags |= CollisionDataFlag.BLOCK_MOVEMENT_EAST;
			xWallFlagsSouth |= CollisionDataFlag.BLOCK_MOVEMENT_SOUTH |
				CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_EAST;
			xWallFlagsNorth |= CollisionDataFlag.BLOCK_MOVEMENT_NORTH |
				CollisionDataFlag.BLOCK_MOVEMENT_NORTH_EAST;
		}
		if (dx > 0)
		{
			xFlags |= CollisionDataFlag.BLOCK_MOVEMENT_WEST;
			xWallFlagsSouth |= CollisionDataFlag.BLOCK_MOVEMENT_SOUTH |
				CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_WEST;
			xWallFlagsNorth |= CollisionDataFlag.BLOCK_MOVEMENT_NORTH |
				CollisionDataFlag.BLOCK_MOVEMENT_NORTH_WEST;
		}
		if (dy < 0)
		{
			yFlags |= CollisionDataFlag.BLOCK_MOVEMENT_NORTH;
			yWallFlagsWest |= CollisionDataFlag.BLOCK_MOVEMENT_WEST |
				CollisionDataFlag.BLOCK_MOVEMENT_NORTH_WEST;
			yWallFlagsEast |= CollisionDataFlag.BLOCK_MOVEMENT_EAST |
				CollisionDataFlag.BLOCK_MOVEMENT_NORTH_EAST;
		}
		if (dy > 0)
		{
			yFlags |= CollisionDataFlag.BLOCK_MOVEMENT_SOUTH;
			yWallFlagsWest |= CollisionDataFlag.BLOCK_MOVEMENT_WEST |
				CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_WEST;
			yWallFlagsEast |= CollisionDataFlag.BLOCK_MOVEMENT_EAST |
				CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_EAST;
		}
		if (dx < 0 && dy < 0)
		{
			xyFlags |= CollisionDataFlag.BLOCK_MOVEMENT_NORTH_EAST;
		}
		if (dx < 0 && dy > 0)
		{
			xyFlags |= CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_EAST;
		}
		if (dx > 0 && dy < 0)
		{
			xyFlags |= CollisionDataFlag.BLOCK_MOVEMENT_NORTH_WEST;
		}
		if (dx > 0 && dy > 0)
		{
			xyFlags |= CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_WEST;
		}

		CollisionData[] collisionData = client.getCollisionMaps();
		int[][] collisionDataFlags = collisionData[plane].getFlags();

		if (dx != 0)
		{
			// Check that the area doesn't bypass a wall
			for (int y = startY; y <= endY; y++)
			{
				if ((collisionDataFlags[checkX][y] & xFlags) != 0 ||
					!extraCondition.test(WorldPoint.fromRegion(client, checkX, y, plane)))
				{
					// Collision while attempting to travel along the x axis
					return false;
				}
			}

			// Check that the new area tiles don't contain a wall
			for (int y = startY + 1; y <= endY; y++)
			{
				if ((collisionDataFlags[checkX][y] & xWallFlagsSouth) != 0)
				{
					// The new area tiles contains a wall
					return false;
				}
			}
			for (int y = endY - 1; y >= startY; y--)
			{
				if ((collisionDataFlags[checkX][y] & xWallFlagsNorth) != 0)
				{
					// The new area tiles contains a wall
					return false;
				}
			}
		}
		if (dy != 0)
		{
			// Check that the area tiles don't bypass a wall
			for (int x = startX; x <= endX; x++)
			{
				if ((collisionDataFlags[x][checkY] & yFlags) != 0 ||
					!extraCondition.test(WorldPoint.fromRegion(client, x, checkY, client.getPlane())))
				{
					// Collision while attempting to travel along the y axis
					return false;
				}
			}

			// Check that the new area tiles don't contain a wall
			for (int x = startX + 1; x <= endX; x++)
			{
				if ((collisionDataFlags[x][checkY] & yWallFlagsWest) != 0)
				{
					// The new area tiles contains a wall
					return false;
				}
			}
			for (int x = endX - 1; x >= startX; x--)
			{
				if ((collisionDataFlags[x][checkY] & yWallFlagsEast) != 0)
				{
					// The new area tiles contains a wall
					return false;
				}
			}
		}
		if (dx != 0 && dy != 0)
		{
			if ((collisionDataFlags[checkX][checkY] & xyFlags) != 0 ||
				!extraCondition.test(WorldPoint.fromRegion(client, checkX, checkY, client.getPlane())))
			{
				// Collision while attempting to travel diagonally
				return false;
			}

			// When the areas edge size is 1 and it attempts to travel
			// diagonally, a collision check is done for respective
			// x and y axis as well.
			if (width == 1)
			{
				if ((collisionDataFlags[checkX][checkY - dy] & xFlags) != 0 &&
					extraCondition.test(WorldPoint.fromRegion(client, checkX, startY, client.getPlane())))
				{
					return false;
				}
			}
			if (height == 1)
			{
				if ((collisionDataFlags[checkX - dx][checkY] & yFlags) != 0 &&
					extraCondition.test(WorldPoint.fromRegion(client, startX, checkY, client.getPlane())))
				{
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Retrieves the Point within this WorldArea which is the closest to another WorldArea
	 *
	 * @param other The other WorldArea to compare to
	 * @return Returns the closest Point
	 */
	private Point getComparisonPoint(WorldArea other)
	{
		int x, y;
		if (other.x <= this.x)
		{
			x = this.x;
		}
		else if (other.x >= this.x + this.width - 1)
		{
			x = this.x + this.width - 1;
		}
		else
		{
			x = other.x;
		}
		if (other.y <= this.y)
		{
			y = this.y;
		}
		else if (other.y >= this.y + this.height - 1)
		{
			y = this.y + this.height - 1;
		}
		else
		{
			y = other.y;
		}
		return new Point(x, y);
	}

	/**
	 * Calculates the next area that will be occupied if this area
	 * attempts to move toward it by using the normal NPC travelling
	 * pattern.
	 *
	 * @param client The client to calculate with
	 * @param target The target area
	 * @param stopAtMeleeDistance Determine if it should stop at melee distance to the target
	 * @return Returns the next occupied area
	 */
	public WorldArea calculateNextTravellingPoint(Client client, WorldArea target,
		boolean stopAtMeleeDistance)
	{
		return calculateNextTravellingPoint(client, target, stopAtMeleeDistance, x -> true);
	}

	/**
	 * Calculates the next area that will be occupied if this area
	 * attempts to move toward it by using the normal NPC travelling
	 * pattern.
	 *
	 * @param client The client to calculate with
	 * @param target The target area
	 * @param stopAtMeleeDistance Determine if it should stop at melee distance to the target
	 * @param extraCondition Additional check for if movement is allowed through specific
	 * tiles, which may be used if movement should be disabled through other actors
	 * @return Returns the next occupied area
	 */
	public WorldArea calculateNextTravellingPoint(Client client, WorldArea target,
		boolean stopAtMeleeDistance, Predicate<? super WorldPoint> extraCondition)
	{
		if (plane != target.getPlane())
		{
			return null;
		}

		if (this.intersectsWith(target))
		{
			if (stopAtMeleeDistance)
			{
				// Movement is unpredictable when the NPC and actor stand on top of each other
				return null;
			}
			else
			{
				return this;
			}
		}

		int dx = target.x - this.x;
		int dy = target.y - this.y;
		Point axisDistances = getAxisDistances(target);
		if (stopAtMeleeDistance && axisDistances.getX() + axisDistances.getY() == 1)
		{
			// NPC is in melee distance of target, so no movement is done
			return this;
		}

		LocalPoint lp = LocalPoint.fromWorld(client, x, y);
		if (lp == null ||
			lp.getRegionX() + dx < 0 || lp.getRegionX() + dy >= Constants.REGION_SIZE ||
			lp.getRegionY() + dx < 0 || lp.getRegionY() + dy >= Constants.REGION_SIZE)
		{
			// NPC is travelling out of region, so collision data isn't available
			return null;
		}

		int dxSig = Integer.signum(dx);
		int dySig = Integer.signum(dy);
		if (stopAtMeleeDistance && axisDistances.getX() == 1 && axisDistances.getY() == 1)
		{
			// When it needs to stop at melee distance, it will only attempt
			// to travel along the x axis when it is standing diagonally
			// from the target
			if (this.canTravelInDirection(client, dxSig, 0, extraCondition))
			{
				return new WorldArea(x + dxSig, y, width, height, plane);
			}
		}
		else
		{
			if (this.canTravelInDirection(client, dxSig, dySig, extraCondition))
			{
				return new WorldArea(x + dxSig, y + dySig, width, height, plane);
			}
			else if (dx != 0 && this.canTravelInDirection(client, dxSig, 0, extraCondition))
			{
				return new WorldArea(x + dxSig, y, width, height, plane);
			}
			else if (dy != 0 && Math.max(Math.abs(dx), Math.abs(dy)) > 1 &&
				this.canTravelInDirection(client, 0, dy, extraCondition))
			{
				// Note that NPCs don't attempts to travel along the y-axis
				// if the target is <= 1 tile distance away
				return new WorldArea(x, y + dySig, width, height, plane);
			}
		}

		// The NPC is stuck
		return this;
	}

	/**
	 * Determine if this WorldArea has line of sight to another WorldArea.
	 * Note that the reverse isn't necessarily true, meaning this can return true
	 * while the other WorldArea does not have line of sight to this WorldArea.
	 *
	 * @param client The client to compare in
	 * @param other The other WorldArea to compare with
	 * @return Returns true if this WorldArea has line of sight to the other
	 */
	public boolean hasLineOfSightTo(Client client, WorldArea other)
	{
		if (plane != other.getPlane())
		{
			return false;
		}

		LocalPoint sourceLp = LocalPoint.fromWorld(client, x, y);
		LocalPoint targetLp = LocalPoint.fromWorld(client, other.getX(), other.getY());
		if (sourceLp == null || targetLp == null)
		{
			return false;
		}

		int thisX = sourceLp.getRegionX();
		int thisY = sourceLp.getRegionY();
		int otherX = targetLp.getRegionX();
		int otherY = targetLp.getRegionY();

		int cmpThisX, cmpThisY, cmpOtherX, cmpOtherY;

		// Determine which position to compare with for this NPC
		if (otherX <= thisX)
		{
			cmpThisX = thisX;
		}
		else if (otherX >= thisX + width - 1)
		{
			cmpThisX = thisX + width - 1;
		}
		else
		{
			cmpThisX = otherX;
		}
		if (otherY <= thisY)
		{
			cmpThisY = thisY;
		}
		else if (otherY >= thisY + height - 1)
		{
			cmpThisY = thisY + height - 1;
		}
		else
		{
			cmpThisY = otherY;
		}

		// Determine which position to compare for the other actor
		if (thisX <= otherX)
		{
			cmpOtherX = otherX;
		}
		else if (thisX >= otherX + other.getWidth() - 1)
		{
			cmpOtherX = otherX + other.getWidth() - 1;
		}
		else
		{
			cmpOtherX = thisX;
		}
		if (thisY <= otherY)
		{
			cmpOtherY = otherY;
		}
		else if (thisY >= otherY + other.getHeight() - 1)
		{
			cmpOtherY = otherY + other.getHeight() - 1;
		}
		else
		{
			cmpOtherY = thisY;
		}

		Tile[][][] tiles = client.getRegion().getTiles();
		Tile sourceTile = tiles[plane][cmpThisX][cmpThisY];
		Tile targetTile = tiles[other.getPlane()][cmpOtherX][cmpOtherY];
		if (sourceTile == null || targetTile == null)
		{
			return false;
		}
		return sourceTile.hasLineOfSightTo(targetTile);
	}

	/**
	 * Determine if this WorldArea has line of sight to another WorldArea.
	 * Note that the reverse isn't necessarily true, meaning this can return true
	 * while the other WorldArea does not have line of sight to this WorldArea.
	 *
	 * @param client The client to compare in
	 * @param other The other WorldPoint to compare with
	 * @return Returns true if this WorldPoint has line of sight to the WorldPoint
	 */
	public boolean hasLineOfSightTo(Client client, WorldPoint other)
	{
		return hasLineOfSightTo(client, new WorldArea(other, 1, 1));
	}

	/**
	 * Retrieves the southwestern most point of this WorldArea
	 *
	 * @return Returns the southwestern most WorldPoint in the area
	 */
	public WorldPoint toWorldPoint()
	{
		return new WorldPoint(x, y, plane);
	}
}