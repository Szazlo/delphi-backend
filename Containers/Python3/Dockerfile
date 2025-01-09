# Use the Python slim image as the base image
FROM python:3.13-slim

# Install unzip
RUN apt-get update && apt-get install -y unzip
RUN pip install pylint

# Set the working directory
WORKDIR /usr/src/app

# Copy the run script into the container
COPY run.sh .

# Make the run script executable
RUN chmod +x run.sh

# Set the default command to execute the run script
CMD ["./run.sh"]
