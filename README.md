# Ex3 – Pac-Man Algorithm Design

## Overview
This document describes the decision-making algorithm of my Pac-Man agent.
At each step, Pac-Man chooses a direction based on the current game state,
with a clear priority order focused on survival and efficient score gain.

## Game Information Used
At every move, Pac-Man considers:
- Its current position
- Positions of all ghosts
- Whether ghosts are dangerous or eatable
- Positions of green power dots
- Positions of pink dots
- The map structure (walls and free cells)

Distances are measured in number of steps on the grid.

## Decision Rules (Priority Order)

### Rule 1 – Escape from Dangerous Ghosts
If there is at least one **dangerous ghost within 5 steps** of Pac-Man,
Pac-Man will immediately try to escape.

The chosen move is the legal direction that increases the distance
from the closest dangerous ghost.

This rule has the highest priority and overrides all others.

### Rule 2 – Move Toward a Green Power Dot
If there are **no dangerous ghosts nearby** and a **green power dot is visible**,
Pac-Man will move toward the nearest green dot in order to gain the ability
to eat ghosts.

### Rule 3 – Chase Eatable Ghosts
If ghosts are currently eatable and at least one eatable ghost is within **5 steps**,
Pac-Man will move toward the closest eatable ghost to maximize score.

### Rule 4 – Eat Pink Dots
If none of the above conditions apply, Pac-Man will continue moving toward
the nearest pink dot to gradually clear the board.

## Pathfinding
For movement decisions, Pac-Man uses shortest-path calculations on the grid
(ignoring walls) and always chooses a legal move.

## Edge Cases
- If no valid path is found, Pac-Man chooses any legal move to avoid being stuck.
- In case of equal distances, the first valid direction is chosen.
