FROM python:3.6-alpine

VOLUME "/data"

RUN pip install flask

ENTRYPOINT [ "flask", "run", "--host", "0.0.0.0" ]

