import os
import shutil


# Read junit_projects.txt
def read_junit_projects():
    with open("junit_projects.txt", "r") as f:
        return f.read().splitlines()


# Pull the latest version of the repository
def clone_repo(repo_url):
    repo_name = repo_url.split("/")[-1]
    repo_path = os.path.join("repos", repo_name)
    if os.path.exists(repo_path):
        shutil.rmtree(repo_path)
    os.system(f"git clone {repo_url} {repo_path}")


# Run the script
if __name__ == "__main__":
    projects = read_junit_projects()
    print(projects)
    for project in projects:
        clone_repo(project)
