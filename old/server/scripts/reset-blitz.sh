#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
pushd $DIR

#
# This script is called once a month to reset the Blitz realm. Basically, we update the current
# rankings and save them off in the historical record, then reset the universe.
#

MYSQL_USER=wwmmo_user
MYSQL_PASSWORD="H98765gf!s876#Hdf2%7f"

# The script is scheduled to run at midnight, but we wait a random amount of time so that we don't
# reset the universe at the same time every month (which could give people an unfair advantage if
# they know when the world will be reset).
MAXDELAY=$((12*60))  # randomly wait up to 12 hours
DELAY=$(($RANDOM%MAXDELAY))
sleep $((DELAY*60))

# Simulate all the stars, so populations are up-to-date
./run-blitz.sh cron simulate-stars 0

# Final update of statistics
./run-blitz.sh cron update-ranks

# Save off the ranks into the history table
read -d '' SCRIPT <<"EOF"
    DELETE FROM empire_rank_histories WHERE date=DATE(NOW());
    INSERT INTO empire_rank_histories
      (date, empire_id, rank, total_stars, total_colonies, total_ships, total_population)
    SELECT (DATE(NOW()) - INTERVAL 1 DAY), empire_id, rank, total_stars,
           total_colonies, total_ships, total_population
    FROM empire_ranks;
EOF
mysql --user=$MYSQL_USER --password=$MYSQL_PASSWORD --batch wwmmo_blitz --execute="$SCRIPT"

# Finally, reset the universe. Clear out the colonies, planets and stars but leave the actual empires
# in place.

read -d '' SCRIPT <<"EOF"
    DELETE FROM scout_reports;
    DELETE FROM combat_reports;
    DELETE FROM situation_reports;
    DELETE FROM build_requests;
    DELETE FROM alliance_join_requests;
    UPDATE empires SET alliance_id = NULL;
    UPDATE empires SET home_star_id = NULL;
    DELETE FROM alliances;
    DELETE FROM buildings;
    DELETE FROM colonies;
    DELETE FROM empire_ranks;
    DELETE FROM empire_presences;
    DELETE FROM fleets;
    DELETE FROM star_renames;
    DELETE FROM stars;
    DELETE FROM sectors;
    UPDATE empires SET reset_reason = 'blitz';
EOF

mysql --user=$MYSQL_USER --password=$MYSQL_PASSWORD --batch wwmmo_blitz --execute="$SCRIPT"

popd
