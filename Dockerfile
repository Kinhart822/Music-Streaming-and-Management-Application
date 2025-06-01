# Declare base image
FROM nginx:alpine

# Set working directory
WORKDIR /usr/share/nginx/html

# Copy files
COPY . .

# Expose nginx port
EXPOSE 80

# Command
CMD ["nginx", "-g", "daemon off;"]
