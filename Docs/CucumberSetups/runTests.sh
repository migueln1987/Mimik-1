#!/usr/bin/env bash

pat_options_s=" (-[[:alpha:]])([^_[:alnum:]]| )" #ex: -k
pat_options_l="(--[[:alnum:]-]+)( ([^- ]+))?" #ex: --tag all or --dry-run
pat_tags="(~?@[[:alnum:]]+(:[[:digit:]]+)?)" #ex: @include ~@exclude @limit:3

#ex: feature: feature:3 feature:3:10 /path/feature:3
#[0]= full match, [1]= (path/)?(feature name) [2]= /?(feature name) [3]= line numbers
pat_feature="((\/?[_[:alnum:]]+)*):([0-9]+(:[0-9]+)*)?"
#ex: feature: feature:3 feature:3:10:15
pat_params="([_[:alnum:]]+)=([_[:alnum:]]+)" # Something=otherSomething
feature_Dirs=("features" "app/src/androidTest/assets/features")
glue_dirs=("features")
#app/src/androidTest/java/

# Regex match lines
#(?'options'-(?:(?'short'\w)|(?'long'-\w{2,} +[\w]+)))
#(?'tag'~?@\w+(:\d+)?)
#(?'Scenario'#(?:(?'named'"(?:[\w ]+)"(?= |$))|(?'ln'\d+(?:[:,]\d+)*)))
#(?'param'\w+=\w+)

<<Usage
    To quickly test single instances of feature files or a line in a feature file

    > sh runTests.sh {cucumber options} {feature[:](test line)?(:test lines)*} {tags}

    == cucumber options ==
    > cucumber --help to display options
Usage

<<Todo
    == high ==
    - Run tests without needing to postfix "." or ":"
    - "-h/-help" documentation
    - Display cucumber help options (-H/-Help)
    - Screenshot after pass/fail (each test?)
    - Handle case when multiple emulators/ devices are running
    - Test support for running on a physical device
    - Tester can include which line to test, which could be a non-scenario line

    == medium ==
    - Appium cli options
    - Option to kill appium (cli or stop gui) to run a new instance of appium (cli or gui)

    == low ==
    - Apply tags to specific features only
    - Tag expression support (AND, combination of AND/OR/NOT/ANY/ALL)
      - https://cucumber.io/docs/cucumber/api/#tag-expressions
    - Detect feature files regardless of spelling cap ("cardOnOff" vs "cardonoff")
    - Run tests based on scenario name (without needing feature file name)
    - Username and password(?) input
    - Add more glue dirs?
    - androidTest is an option, but test if those tests can be run too
Todo

main() {
  if [[ $1 == '-tests' ]]; then
    echo "==== Avaliable tests ===="
    for dir in ${feature_Dirs[@]}; do
      echo "=== Directory: $dir"
      getFeatureFiles $dir
    done
    exit 1
  fi

  ensure_Requirements
  check_app_exists $*

  [[ $# -lt 1 ]] && echo "no arguments found" && exit 1

  run_script=""
  parse_options $*
  parse_tags $*
  parse_features $*

  if [[ $1 == ? ]]; then
    echo "example 1: 'login:3' = Runs test (line 3 from login.feature)"
    echo "example 2: 'cardOnOff:3:11'"
    #        echo dir:
    #        pwd
    #        echo script: ${run_script}
  fi

  if [[ ! -z ${run_script} ]]; then
    start_appium
    #        grab_device
    parse_params $* # input can override default (emulator) device
    [[ -d results ]] || mkdir results
    run_script+="-r features --format html --out=results/results.html --format pretty -p localsystem "
    #        run_script+="glue
    run_script+="glue={\"${glue_dirs[@]}\"} "
    #        run_script+="-glue Steps "

    if [[ ! -z $(which bundler) ]]; then
      echo "Script: bundler exec cucumber ${run_script}"
      bundler exec cucumber ${run_script}
    else
      echo "Script: cucumber ${run_script}"
      cucumber ${run_script}
    fi
  else
    echo "no cucumber params"
  fi
}

ensure_Requirements() {
  local hasMinium=true

  [[ -z $(which appium) || -z $(lsof -n -i:4723 | grep LISTEN) ]] && { echo "Appium not installed"; hasMinium=false; }
  [[ -z $(which cucumber) ]] && { echo "Cucumber not installed"; hasMinium=false; }

  #    [[ -z $(which ruby) ]] && echo "Ruby not installed"
  [[ -z $(which bundler) ]] && echo "Bundler not installed"

  [[ ! hasMinium ]] && { echo "Missing requirements; exiting now"; exit 1; }
}

platform_type() {
  if [[ -f gradlew ]]; then
    echo 'android'
  elif [[ -e CardValet.xcworkspace ]]; then
    echo 'ios'
  else
    echo 'unknown'
  fi
}

try_build=false
check_app_exists() {
  build="debug"
  flavor="demo"

  pat_build="Build=([[:alpha:]]+)"
  pat_flavor="Flavor=([[:alpha:]]+)"

  [[ $@ =~ $pat_build ]] && build="${BASH_REMATCH[1],,}"
  [[ $@ =~ $pat_flavor ]] && flavor="${BASH_REMATCH[1],,}"

  app_location=""
  build_cmd=""

  echo "Platform: $(platform_type)"
  case $(platform_type) in
    'android')
      app_location="app/build/outputs/apk/$flavor/$build/app-$flavor-$build.apk"
      build_cmd="./gradlew 'assemble$flavor$build'"
      ;;

    'ios')
      build="iOS"
      caps="$(tr '[:lower:]' '[:upper:]' <<< ${flavor:0:1})${flavor:1}"
      all_upper=$(echo ${flavor} | tr '[:lower:]' '[:upper:]')

      app_name="CardValet${caps}.app"
      app_path="build/Build/Products/${all_upper}-iphonesimulator"
      app_location="${PWD}/${app_path}/${app_name}"
      build_cmd="xcodebuild -workspace CardValet.xcworkspace -scheme CardValet${all_upper} -sdk iphonesimulator -derivedDataPath "$(pwd)/build" build"
      ;;
  esac

  if [[ ! -z app_location && ! -e ${app_location} ]]; then
    echo "Build: $build, Flavor: $flavor"
    echo "App does not exist; attempting to build the project now."

    try_build=true
    eval ${build_cmd}
    if [[ ! -z app_location && ! -e ${app_location} ]]; then
      echo "Unable to build the package exiting now"
      exit 1
    fi
  else
    echo "== App is ready =="
  fi
}

parse_options() {
  # == Input == Cucumber options
  # "-x" or "--x yyyy"

  local s=" $@ "
  while [[ ${s} =~ $pat_options_s ]]; do
    run_script+="${BASH_REMATCH[0]} "
    s=${s#*"${BASH_REMATCH[0]}"}
  done

  s=" $@ "
  while [[ ${s} =~ $pat_options_l ]]; do
    if [[ ${BASH_REMATCH[0]} == *:* ]]; then
      run_script+="${BASH_REMATCH[1]} "
      s=${s#*"${BASH_REMATCH[1]}"}
    else
      run_script+="${BASH_REMATCH[0]} "
      s=${s#*"${BASH_REMATCH[0]}"}
    fi
  done
}

parse_tags() {
  # add tags
  # "@tag", "@tagA @tagB" (AND), "~@skip"
  # "@tag:##" = limit to ## instances of "tag"

  # note: ignoring (OR) "@tagA,@tagB"
  unset tag_params
  for args in $*; do
    if [[ ${args} =~ $pat_tags ]]; then
      tag_params+=' '${BASH_REMATCH[0]}
    fi
  done
  if [[ ${#tag_params} -gt 0 ]]; then
    run_script+="-t $tag_params "
  fi
}

getFeatureFiles() {
  if [[ ! -d $1 ]]; then return; fi

  read -r -a features <<< $(ls -R $1)
  subDir=""
  validDir=true

  for index in "${!features[@]}" ; do
    item=${features[$index]}

    if [[ ${item} == *".feature" && validDir ]]; then
      if [[ -z ${subDir} ]]; then
        echo ${item}
      else
        echo ${subDir}${item}
      fi
    elif [[ ${item} == *":" ]]; then
      subDir=${item//:/\/}
      validDir=true
    else
      validDir=false
      # {current directory} / ($1/ or subDir) / item
      dirTest=$PWD"/"${subDir:-$1"/"}$item
      [[ ! -d $dirTest ]] && echo "*** invalid directory: "$dirTest
    fi
  done
}

parse_features() {
  # == Input ==
  # "feature:" => run whole feature file
  # "feature:##" => run scenario at line ##
  # "feature:##:##:##" -> run scenario at lines
  # "path/path/feature:##:##:##" -> run scenario at lines

  unset feature_obj
  for dir in ${feature_Dirs[@]}; do # grab correct feature file dir
    files=$(getFeatureFiles $dir)
    if [[ -z $files || ${#files[@]} -eq 0 ]]; then
      #      echo "Dir ["$dir"] has no files"
      break
    fi

    for args in $*; do
      #echo "args: " ${args}
      if [[ $args =~ $pat_feature ]]; then
        name=${BASH_REMATCH[1]}
        lines=${BASH_REMATCH[3]}
        search="(^|\/)${name}.feature"
        foundFile=''

        # attempt to find as was entered
        for file in ${files[@]}; do
          echo 'Compare: '$file' to '$search
          if [[ ${file} =~ $search ]]; then
            if [[ ${file} == ${dir}* ]]; then
              # "file" is complete with a "dir" prefix
              foundFile=${file}
            else # append "dir" prefix
              foundFile=${dir}/${file}
            fi
            #echo 'Matched input at: '$foundFile
            break
          fi
        done

        # append file to pre-script (opt. line numbers)
        if [[ ! -z $foundFile && -f $foundFile ]]; then
          feature_obj=$foundFile
          [[ ! -z $lines ]] && feature_obj+=:$lines # add line numbers
          #echo 'Adding test of: '$feature_obj
        fi

        # append ready pre-script to script
        if [[ ! -z $feature_obj ]]; then
          echo "+feature: $feature_obj"
          run_script+="$feature_obj "
        else
          echo "No matching feature found for: '$name'"
        fi
      fi
    done
  done
}

parse_params() {
  local s=" $@"

  while [[ ${s} =~ $pat_params ]]; do
    run_script+="${BASH_REMATCH[1]}=${BASH_REMATCH[2]} "
    #        ENV[${BASH_REMATCH[1]}]=${BASH_REMATCH[2]}
    s=${s#*"${BASH_REMATCH[0]}"}
  done
}

start_appium() {
  # Check Appium is running
  if [[ -z $(lsof -n -i:4723 | grep LISTEN) ]]; then
    echo "Starting Appium..."
    appium &
    # uncomment to include custom appium cli options
    # appium --address 127.0.0.1 --chromedriver-port 9516 --bootstrap-port 4725 --no-reset --relaxed-security &
    sleep 10s
  else
    echo "Appium is already running... "
  fi
}

grab_device() {
  devices = $ANDROID_HOME/platform-tools/adb devices | awk -F emulator "NF > 1"
  if [[ -z ${devices} ]]; then
    # emulator is active, use this as the active testing device
    echo "Android Emulator"
    ENV[DeviceName]="Android Emulator"
    ENV[avd]=""
  else
    echo "load AVD"
  fi

  if [[ ! -z ENV[DeviceName] ]]; then
    run_script+="OS=Android "
    run_script+="DeviceName=${ENV[DeviceName]} avd=${ENV[avd]} "
  fi
}

main "$@"
