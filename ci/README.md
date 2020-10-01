# CI Scripts

These scripts are used in the CI (Github Actions) build.

## Using `act` to Run CI Locally

The [`act`][act] tool can be used to run (an
approximation of) the Github Actions workflow locally:

- Install `act`
- Run `act --env-file "" -P ubuntu-latest=nektos/act-environments-ubuntu:18.04`

The Docker image `nektos/act-environments-ubuntu:18.04` is quite large
(approximately 18GB), so (1) you'll need enough free disk space to store it and
(2) the first execution of `act` takes a while because it downloads this image.

### Troubleshooting

#### "volume is in use"

If you see a message like this:
```
Error: Error response from daemon: remove act-Build-Build-SemTK: volume is in use
```
You can forcibly stop and remove the `act` Docker containers and their volumes:
```bash
docker stop $(docker ps -a | grep "nektos/act-environments-ubuntu:18.04" | awk '{print $1}')
docker rm $(docker ps -a | grep "nektos/act-environments-ubuntu:18.04" | awk '{print $1}')
docker volume rm $(docker volume ls --filter dangling=true | grep -o -E "act-.+$")
```
There may also be a more precise solution to this issue, but the above works.

#### "permission denied while trying to connect to the Docker daemon socket"

`act` needs to be run with enough privileges to run Docker containers. Try
`sudo -g docker act ...` (or an equivalent invocation for your OS/distro).

[act]: (https://github.com/nektos/act)
