FROM ubuntu:24.10

ENV TZ=Asia/Kolkata
# RUN apt update
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
RUN apt-get -o Acquire::Check-Valid-Until=false -o Acquire::Check-Date=false update && apt-get clean

RUN apt-get install software-properties-common -y
RUN add-apt-repository ppa:openjdk-r/ppa

RUN apt-get -y install openjdk-11-jdk wget

RUN mkdir /home/installer
COPY installer /home/installer

RUN mkdir /home/benchmarks
COPY benchmarks /home/benchmarks

RUN mkdir /home/xdata
COPY xdata /home/xdata

RUN mkdir /home/xdata/inputs

RUN mkdir /home/xdata/tmp
RUN mkdir /home/xdata/vrtuenv
RUN mkdir /home/xdata/output

RUN sed -i -e 's/\r$//' /home/installer/installer.sh

RUN chmod +x /home/installer/installer.sh

RUN apt-get install -y z3
RUN apt-get install libz3-4
RUN apt-get install -y libz3-java
RUN apt-get install -y vim

RUN apt-get install -y postgresql
RUN update-rc.d postgresql defaults

RUN apt-get install -y python3
RUN apt-get install -y python3-pip
RUN python3 --version
RUN apt install -y python3-venv
RUN python3 -m venv /home/xdata/vrtuenv
RUN chmod +x /home/xdata/vrtuenv/bin/activate
RUN  /home/xdata/vrtuenv/bin/activate

RUN /home/xdata/vrtuenv/bin/pip install -r /home/installer/requirements.txt

# RUN pip install jpype1

RUN /home/installer/installer.sh
ENTRYPOINT service postgresql start >/dev/null 2>&1 | cat && bash

WORKDIR /home/xdata

# RUN source vrtuenv/bin/activate