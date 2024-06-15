# Use the official lightweight Python image
FROM python:3.9-alpine

# Set the working directory
WORKDIR /app

# Copy the Python script into the container
COPY yad2-bumper.py .

# Install the required Python packages
RUN pip install --no-cache-dir requests pytz

# Set the entrypoint to the Python script
ENTRYPOINT ["python", "yad2-bumper.py"]