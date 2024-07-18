import json
import requests
import os
import xml.etree.ElementTree as ET
from tqdm import tqdm

# Set your GitHub personal access token here
GITHUB_TOKEN = os.getenv("GITHUB_TOKEN")

# GitHub API headers
headers = {
    "Authorization": f"token {GITHUB_TOKEN}",
    "Accept": "application/vnd.github.v3+json",
}


# Function to search for Java projects with pom.xml files
def search_java_projects(pages=5, per_page=30):
    query = "+filename:pom.xml"
    url = f"https://api.github.com/search/code?q={query}"
    response = requests.get(url, headers=headers)
    response.raise_for_status()
    total_count = response.json()["total_count"]
    print(f"Found {total_count} projects with pom.xml files.")

    items = response.json()["items"]
    pages = min(total_count // per_page + 1, pages)  # Limit pages to available results

    for current_page in tqdm(range(2, pages + 1), desc="Fetching results"):
        params = {"page": current_page, "per_page": per_page}
        response = requests.get(url, headers=headers, params=params)
        response.raise_for_status()
        items.extend(response.json()["items"])

    return items


# Function to download a file from GitHub
def download_file(url):
    response = requests.get(url, headers=headers)
    response.raise_for_status()
    return response.text


# Function to download a file from GitHub
def download_pom_from_item(url):
    response = requests.get(url, headers=headers)
    response.raise_for_status()
    return download_file(json.loads(response.text)["download_url"])


# Function to parse pom.xml and check for JUnit dependencies
def has_junit_dependency(pom_content):
    root = ET.fromstring(pom_content)
    namespaces = {"mvn": "http://maven.apache.org/POM/4.0.0"}
    for dependency in root.findall(".//mvn:dependency", namespaces):
        group_id = dependency.find("mvn:groupId", namespaces)
        artifact_id = dependency.find("mvn:artifactId", namespaces)
        version = dependency.find("mvn:version", namespaces)
        if group_id is not None and artifact_id is not None and version is not None:
            if (
                group_id.text == "junit"
                and artifact_id.text == "junit"
                and version.text.startswith("4.")
            ):
                return True
    return False


# Main function to collect projects
def collect_projects():
    projects = []
    search_results = search_java_projects()
    # Write search results to log file
    for item in tqdm(search_results, desc="Processing results"):
        repo_url = item["repository"]["html_url"]
        pom_url = item["url"]
        try:
            pom_content = download_pom_from_item(pom_url)
            if has_junit_dependency(pom_content):
                projects.append(repo_url)
        except Exception as e:
            print(f"Error processing {pom_url}: {e}")
    return projects


# Run the script
if __name__ == "__main__":
    projects = collect_projects()
    with open("junit_projects.txt", "w") as f:
        for project in projects:
            f.write(f"{project}\n")
    print(f"Collected {len(projects)} projects with JUnit 4 dependencies.")
