const isLocalTest = localStorage.getItem("localtest") === "true";
if (isLocalTest) document.getElementById("dev").style.display = "block";
const token = localStorage.getItem("token");

if (!token && !isLocalTest) {
  window.location.href = "./admin-signin.html";
}

const lib = {
  whitelist: {
    get: async () => {
      if (isLocalTest) return ["crc123", "crc456", "crc789"];
      const response = await fetch("/adminapi/whitelist?token=" + token, {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
        },
      });
      if (!response.ok) throw new Error("Failed to fetch whitelist");
      return response.json();
    },
    add: async (playerID) => {
      if (isLocalTest) return;
      const response = await fetch(
        "/adminapi/whitelist/add?token=" + token + "&pid=" + playerID,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
        }
      );
      if (!response.ok) throw new Error("Failed to add player to whitelist");
      return;
    },
    remove: async (playerID) => {
      if (isLocalTest) return;
      const response = await fetch(
        "/adminapi/whitelist/remove?token=" + token + "&pid=" + playerID,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
        }
      );
      if (!response.ok)
        throw new Error("Failed to remove player from whitelist");
      return;
    },
  },
  kv: {
    nickname: async (playerID) => {
      // /adminapi/kv/playerinfo-nickname
      if (isLocalTest) return "TestNickname";
      const response = await fetch(
        `/adminapi/kv/playerinfo-nickname?token=${token}&pid=${playerID}`,
        {
          method: "GET",
          headers: {
            "Content-Type": "application/json",
          },
        }
      );
      if (!response.ok) {
        throw new Error("Failed to fetch nickname");
      }
      const data = await response.text();
      return data ? data : "No nickname set";
    },
    team: async (playerID) => {
      // /adminapi/kv/playerinfo-team
      if (isLocalTest) return "-1";
      const response = await fetch(
        `/adminapi/kv/playerinfo-team?token=${token}&pid=${playerID}`,
        {
          method: "GET",
          headers: {
            "Content-Type": "application/json",
          },
        }
      );
      if (!response.ok) {
        throw new Error("Failed to fetch team");
      }
      const data = await response.text();
      return data;
    },
  },
  team: {
    list: async () => {
      // /adminapi/kv/teams
      if (isLocalTest)
        return {
          "team-a-123123123": {
            name: "Team A",
            masterPid: "crc123",
          },
          "team-b-456456456": {
            name: "Team B",
            masterPid: "crc456",
          },
          "team-c-789789789": {
            name: "Team C",
            masterPid: "crc789",
          },
        };
      const response = await fetch(`/adminapi/kv/teams?token=${token}`, {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
        },
      });

      if (!response.ok) {
        throw new Error("Failed to fetch teams");
      }
      const data = await response.json();
      return data;
    },
    setTeamForPlayer: async (playerID, teamIndex) => {
      // POST /adminapi/kv/playerinfo-team/set
      if (isLocalTest) return;
      const response = await fetch(
        `/adminapi/kv/playerinfo-team/set?token=${token}&pid=${playerID}&team=${teamIndex}`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
        }
      );
      if (!response.ok) {
        throw new Error("Failed to set team");
      }
    },
    setTeamData: async (teamId, teamName, masterPID) => {
      // POST /adminapi/teams/update
      if (isLocalTest) return;
      const response = await fetch(
        `/adminapi/teams/update?token=${token}&teamId=${teamId}&name=${teamName}&masterPid=${masterPID}`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
        }
      );
      if (!response.ok) {
        throw new Error("Failed to set team data");
      }
    },
    create: async (teamName, masterPid) => {
      // POST /adminapi/teams/create
      if (isLocalTest) return;
      const response = await fetch(
        `/adminapi/teams/create?token=${token}&name=${teamName}&masterPid=${masterPid}`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
        }
      );
      if (!response.ok) {
        throw new Error("Failed to create team");
      }
    },
    delete: async (teamId) => {
      // POST /adminapi/teams/delete
      if (isLocalTest) return;
      const response = await fetch(
        `/adminapi/teams/delete?token=${token}&teamId=${teamId}`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
        }
      );
      if (!response.ok) {
        throw new Error("Failed to delete team");
      }
    },
  },
};

(async () => {
  const teamMap = await lib.team.list();

  // Whitelist fetcher
  (() => {
    function generatePlayerListItem(playerID) {
      const tr = document.createElement("tr");
      const td = document.createElement("td");
      td.textContent = playerID;
      tr.appendChild(td);

      const td2 = document.createElement("td");
      td2.textContent = "Loading...";
      tr.appendChild(td2);

      const td3 = document.createElement("td");
      td3.textContent = "Loading...";
      tr.appendChild(td3);

      const td4 = document.createElement("td");
      const button = document.createElement("button");
      button.textContent = "Remove";
      button.className = "button";
      td4.appendChild(button);
      tr.appendChild(td4);

      button.addEventListener("click", () => {
        if (!confirm(`Are you sure you want to remove player ${playerID}?`))
          return;
        lib.whitelist
          .remove(playerID)
          .then(() => {
            alert(`Player ${playerID} removed from whitelist successfully.`);
            window.location.reload();
          })
          .catch((error) => {
            console.error("Error removing player from whitelist:", error);
            alert(`Failed to remove player ${playerID} from whitelist.`);
          });
      });

      function fetchData() {
        lib.kv
          .nickname(playerID)
          .then((nickname) => {
            td2.textContent = nickname;
          })
          .catch((error) => {
            console.error("Error fetching nickname:", error);
            td2.textContent = "[?] Unknown";
          });

        lib.kv
          .team(playerID)
          .then((teamID) => {
            td3.innerHTML = "";
            const select = document.createElement("select");
            select.className = "team-select";
            const noTeamOption = document.createElement("option");
            noTeamOption.value = "-1";
            noTeamOption.textContent = "No Team";
            select.appendChild(noTeamOption);

            Object.entries(teamMap).forEach(([teamKey, teamName], index) => {
              const option = document.createElement("option");
              option.value = teamKey;
              option.textContent = teamName.name;
              select.appendChild(option);
            });

            select.value = teamID || "-1"; // Set to No Team if no team is set
            select.addEventListener("change", () => {
              const selectedTeam = select.value;
              lib.team
                .setTeamForPlayer(playerID, selectedTeam)
                .then(() => {
                  alert(`Team for player ${playerID} updated successfully.`);
                })
                .catch((error) => {
                  console.error("Error setting team:", error);
                  alert(`Failed to update team for player ${playerID}.`);
                });
            });
            td3.appendChild(select);
          })
          .catch((error) => {
            console.error("Error fetching team:", error);
            td3.textContent = "[?] Unknown";
          });
      }

      // when the div is visible, fetch the data
      const observer = new IntersectionObserver(
        (entries) => {
          entries.forEach((entry) => {
            if (entry.isIntersecting) {
              console.log(`Fetching data for player ${playerID}`);
              fetchData();
              observer.unobserve(entry.target); // Stop observing after fetching
            }
          });
        },
        { threshold: 0.1 } // Adjust threshold as needed
      );
      observer.observe(tr);

      return tr;
    }
    const pmsg = document.getElementById("players-message");
    lib.whitelist
      .get()
      .then((whitelist) => {
        if (whitelist.length === 0) {
          pmsg.innerHTML = "There are no players at the moment.";
        } else {
          pmsg.style.display = "none";
          const plist = document.getElementById("players-list");
          whitelist.forEach((playerID) => {
            plist.appendChild(generatePlayerListItem(playerID));
          });
        }
      })
      .catch((error) => {
        console.error("Error fetching whitelist:", error);
        pmsg.innerHTML = "Error fetching whitelist";
      });
  })();

  // Whitelist adder
  (() => {
    const form = document.getElementById("whitelist-form");
    form.addEventListener("submit", (event) => {
      event.preventDefault();
      const playerID = document.getElementById("playerHash").value.trim();
      if (!playerID) {
        alert("Please enter a player ID.");
        return;
      }
      lib.whitelist
        .add(playerID)
        .then(() => {
          alert("Player added to whitelist successfully.");
          window.location.reload();
        })
        .catch((error) => {
          console.error("Error adding player to whitelist:", error);
          alert("Failed to add player to whitelist.");
        });
    });
  })();

  // Create team
  (() => {
    const form = document.getElementById("team-create-form");
    form.addEventListener("submit", (event) => {
      event.preventDefault();
      const teamName = document.getElementById("teamname").value.trim();
      const masterPid = document.getElementById("masterPid").value.trim();
      if (!teamName || !masterPid) {
        alert("Please enter a team name and master player ID.");
        return;
      }
      lib.team
        .create(teamName, masterPid)
        .then(() => {
          alert("Team created successfully.");
          window.location.reload();
        })
        .catch((error) => {
          console.error("Error creating team:", error);
          alert("Failed to create team.");
        });
    });
  })();

  // Fetch and display teams
  (() => {
    const teamList = document.getElementById("team-list");
    const teamMsg = document.getElementById("team-message");

    if (Object.keys(teamMap).length === 0) {
      teamMsg.innerHTML = "There are no teams at the moment.";
      return;
    }

    teamMsg.style.display = "none";
    Object.entries(teamMap).forEach(([teamId, teamData]) => {
      const tr = document.createElement("tr");

      const td1 = document.createElement("td");
      td1.textContent = teamId;
      tr.appendChild(td1);

      const td2 = document.createElement("td");
      tr.appendChild(td2);

      const td3 = document.createElement("td");
      tr.appendChild(td3);

      const td4 = document.createElement("td");
      tr.appendChild(td4);

      const switchToViewMode = () => {
        td2.innerHTML = "";
        const nameSpan = document.createElement("span");
        nameSpan.textContent = teamData.name;
        nameSpan.style.cursor = "pointer";
        nameSpan.addEventListener("click", switchToEditMode);
        td2.appendChild(nameSpan);

        td3.innerHTML = "";
        const pidSpan = document.createElement("span");
        pidSpan.textContent = teamData.masterPid;
        pidSpan.style.cursor = "pointer";
        pidSpan.addEventListener("click", switchToEditMode);
        td3.appendChild(pidSpan);

        td4.innerHTML = "";
        const deleteButton = document.createElement("button");
        deleteButton.textContent = "Delete";
        deleteButton.className = "button button-danger";
        deleteButton.addEventListener("click", () => {
          if (
            !confirm(`Are you sure you want to delete team ${teamData.name}?`)
          )
            return;
          lib.team
            .delete(teamId)
            .then(() => {
              alert(`Team ${teamData.name} deleted successfully.`);
              window.location.reload();
            })
            .catch((error) => {
              console.error("Error deleting team:", error);
              alert(`Failed to delete team ${teamData.name}.`);
            });
        });
        td4.appendChild(deleteButton);
      };

      const switchToEditMode = () => {
        // Team Name Input
        td2.innerHTML = "";
        const nameInput = document.createElement("input");
        nameInput.type = "text";
        nameInput.value = teamData.name;
        nameInput.className = "input-field";
        nameInput.style.background = "#BAB5A1";
        nameInput.style.color = "#454138";
        nameInput.placeholder = "Team Name";
        nameInput.style.width = "100%";
        td2.appendChild(nameInput);

        // Master PID Input
        td3.innerHTML = "";
        const masterPidInput = document.createElement("input");
        masterPidInput.type = "text";
        masterPidInput.value = teamData.masterPid;
        masterPidInput.className = "input-field";
        masterPidInput.style.background = "#BAB5A1";
        masterPidInput.style.color = "#454138";
        masterPidInput.placeholder = "Master Player ID";
        masterPidInput.pattern = "^[a-zA-Z0-9]+$"; // Alphanumeric pattern
        masterPidInput.title = "Master Player ID must be alphanumeric.";
        masterPidInput.style.width = "100%";
        td3.appendChild(masterPidInput);

        // Actions
        td4.innerHTML = "";
        const applyButton = document.createElement("button");
        applyButton.textContent = "Apply";
        applyButton.className = "button";
        applyButton.addEventListener("click", () => {
          const newName = nameInput.value.trim();
          const newMasterPid = masterPidInput.value.trim();

          if (!newName || !newMasterPid) {
            alert("Team name and master PID cannot be empty.");
            return;
          }
          lib.team
            .setTeamData(teamId, newName, newMasterPid)
            .then(() => {
              alert(`Team ${newName} updated successfully.`);
              window.location.reload();
            })
            .catch((error) => {
              console.error("Error updating team:", error);
              alert(`Failed to update team ${teamData.name}.`);
            });
        });
        td4.appendChild(applyButton);

        const cancelButton = document.createElement("button");
        cancelButton.textContent = "Cancel";
        cancelButton.className = "button";
        cancelButton.style.marginLeft = "4px";
        cancelButton.addEventListener("click", switchToViewMode);
        td4.appendChild(cancelButton);

        nameInput.focus();
      };

      switchToViewMode();
      teamList.appendChild(tr);
    });
  })();
})();
