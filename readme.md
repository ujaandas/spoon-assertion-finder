# Spoon Assertion Finder

This project is called "Assertion Finder" and it aims to automate the process of finding assertions in Java projects.

## Getting Started

To get started with this project, follow the steps below:

1. Use `github_project_miner.py` to generate a file named `junit_projects.txt`. I have included one as an example (but it only collected 5 pages worth of Github results). This file will contain a list of Java projects that use JUnit.

2. Clone the repositories listed in `junit_projects.txt` into the `/repo` folder using the `clone_repo.py` script provided in this project.

Alternatively, you can set the path to your own repositories folder inside the `assertionFinder` script.

## Usage

To use this project, follow these steps:

1. Make sure you have Java 17 and Python installed on your machine.

2. Run the FindAssert class inside assertionFinder (and ensure it points to the right repo folder).
