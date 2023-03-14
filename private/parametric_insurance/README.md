# express-controller code
This project handles the parametric insurance use case.

## Install 
`npm install`
## Startup
`NODE_ENV=development npm start`
## Logging
### Logging level
`NODE_ENV=development LOGGING_LEVEL=debug npm start`
### Logging directory
The logging directory is specified using the `LOGGING_DIRECTORY` variable in the config file.

## Configuration
Uses separate config files in `/config` for each environment i.e `development`,`test`, and `production`

## Running app in daemon mode
*  Set the environment flags
1.  export NODE_ENV=development
2.  export LOGGING_LEVEL=debug
*  Run the command:
`pm2 start ./bin/www --name dev-app -e dev-err.log -o dev-out.log`

## pm2 tool reference
http://pm2.keymetrics.io/docs/usage/quick-start/
