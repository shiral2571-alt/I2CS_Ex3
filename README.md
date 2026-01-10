###pac-Man Algorithm – Overview

This document describes the decision-making algorithm of my Pac-Man agent.
At each step, Pac-Man selects a movement direction based on the current game state, using a strict priority-based rule system focused on survival first and efficient score collection.

###Game Information Used

At every move, Pac-Man considers the following information:

Its current position on the board

Positions of all ghosts

Whether each ghost is dangerous or eatable

Remaining eatable time for ghosts

Positions of green power dots

Positions of pink dots

The map structure (walls and free cells)

Grid-based distances computed using BFS (shortest-path search)

All distances are measured in number of grid steps.

###Decision Rules (Priority Order)

Pac-Man applies the following rules in order.
The first rule whose condition is satisfied determines the move.

###Rule 1 – Escape from Dangerous Ghosts (Highest Priority)

If at least one non-eatable (dangerous) ghost is detected within a predefined danger radius, Pac-Man immediately switches to escape mode.

Pac-Man selects the legal move that maximizes the distance from the closest dangerous ghost.

This rule overrides all other rules and is always checked first.

###Rule 2 – Eat Eatable Ghosts (Safe Chase Only)

If no dangerous ghosts are nearby and at least one ghost is currently eatable, Pac-Man may attempt to chase it only if it is safe.

Safety conditions include:

The ghost is reachable before the eatable timer expires.

A time margin is preserved to avoid being trapped when the ghost becomes dangerous again.

If these conditions are met, Pac-Man moves toward the closest safe eatable ghost.

###Rule 3 – Move Toward Green Power Dots

If there is no immediate danger and no safe eatable ghost to chase, Pac-Man checks for green power dots.

If a green dot exists within a predefined radius, Pac-Man moves toward the closest one.

This allows Pac-Man to gain the ability to eat ghosts and increase future scoring opportunities.

###Rule 4 – Eat Pink Dots (Default Behavior)

If none of the above conditions apply, Pac-Man continues to clear the board by moving toward the nearest pink dot.

This rule acts as the default behavior and is responsible for steady progress through the level.

###Pathfinding

All movement decisions are based on shortest-path (BFS) calculations on the grid while respecting walls and board boundaries (including cyclic boards when enabled).

Pac-Man always chooses a legal move that minimizes the distance to the selected target.

###Edge Cases and Stability

If no valid path to a target is found, Pac-Man selects any legal move to avoid getting stuck.

A short position history is maintained to reduce oscillations and local loops.

When multiple directions are equally optimal, a consistent legal direction is chosen.

###Notes

Not all rules necessarily trigger in every game run.
Some rules depend on specific game situations (ghost proximity, eatable timing, green dot location).
Even if a rule does not activate during a specific run, its logic is fully implemented and evaluated at every step.
