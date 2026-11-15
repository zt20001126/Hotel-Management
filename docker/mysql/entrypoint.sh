#!/bin/sh
set -eu

case "${DATABASE_URL:-}" in
  jdbc:mysql://*) ;;
  *) echo "DATABASE_URL must start with jdbc:mysql://" >&2; exit 1 ;;
esac

url_without_scheme=${DATABASE_URL#jdbc:mysql://}
database_and_options=${url_without_scheme#*/}
database=${database_and_options%%\?*}
options=${database_and_options#*\?}

get_option() {
  option_name=$1
  old_ifs=$IFS
  IFS='&'
  for option in $options; do
    case "$option" in
      "$option_name"=*)
        IFS=$old_ifs
        printf '%s' "${option#*=}"
        return 0
        ;;
    esac
  done
  IFS=$old_ifs
  return 1
}

MYSQL_DATABASE=$database
MYSQL_USER=$(get_option user) || { echo "DATABASE_URL is missing the user parameter" >&2; exit 1; }
MYSQL_PASSWORD=$(get_option password) || { echo "DATABASE_URL is missing the password parameter" >&2; exit 1; }
export MYSQL_DATABASE MYSQL_USER MYSQL_PASSWORD

exec /usr/local/bin/docker-entrypoint.sh "$@"
