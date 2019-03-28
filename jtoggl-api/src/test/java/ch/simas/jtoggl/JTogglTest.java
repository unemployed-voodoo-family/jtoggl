/*
 * jtoggl - Java Wrapper for Toggl REST API https://www.toggl.com/public/api
 *
 * Copyright (C) 2011 by simas GmbH, Moosentli 7, 3235 Erlach, Switzerland
 * http://www.simas.ch
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.simas.jtoggl;

import java.text.SimpleDateFormat;
import java.util.*;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Simon Martinelli
 */
public class JTogglTest {

    private static JToggl jToggl;
    private static TimeEntry timeEntry;
    private static Client client;
    private static Project project;
    private static Task task;
    private static Workspace workspace;

    @BeforeClass
    public static void beforeClass() throws Exception {
        String togglApiToken = System.getenv("TOGGL_API_TOKEN");
        if (togglApiToken == null) {
            togglApiToken = System.getProperty("TOGGL_API_TOKEN");
            if (togglApiToken == null) {
                throw new RuntimeException("TOGGL_API_TOKEN not set.");
            }
        }
        jToggl = new JToggl(togglApiToken, "api_token");
        jToggl.setThrottlePeriod(500l);
        jToggl.switchLoggingOn();

        LinkedHashMap<Long, Workspace> workspaces = (LinkedHashMap<Long, Workspace>) jToggl.getWorkspaces();
        assertTrue(workspaces.size() > 0);
        workspace = workspaces.entrySet().iterator().next().getValue();
        client = createClient();
        project = createProject();
        timeEntry = createTimeEntry(project);
        //task = createTask();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        jToggl.destroyTimeEntry(timeEntry.getId());
        jToggl.destroyClient(client.getId());
        try {
            jToggl.destroyTask(task.getId());
        } catch (Exception e) {
            // Ignore because Task is only for paying customers
        }
        jToggl.destroyProject(project.getId());
    }

    @Test
    public void getTimeEntries() {
        List<TimeEntry> entries = jToggl.getTimeEntries();

        Assert.assertFalse(entries.isEmpty());
    }

    @Test
    public void getTimeEntriesWithRange() {
        Calendar cal = Calendar.getInstance();
        cal.set(2011, 11, 10);
        List<TimeEntry> entries = jToggl.getTimeEntries(cal.getTime(), cal.getTime());

        Assert.assertTrue(entries.isEmpty());
    }

    @Test
    public void getTimeEntriesWithRange2() {
        List<TimeEntry> entries = jToggl.getTimeEntries(timeEntry.getStart(), timeEntry.getStop());

        Assert.assertTrue(!entries.isEmpty());
    }

    @Test
    public void getTimeEntry() {
        TimeEntry te = jToggl.getTimeEntry(timeEntry.getId());

        Assert.assertNotNull(te);
    }

    @Test
    public void getMissingTimeEntry() {
        TimeEntry te = jToggl.getTimeEntry(1l);

        Assert.assertNull(te);
    }

    @Test
    public void updateTimeEntry() throws Exception {
        TimeEntry timeEntry = createTimeEntry(project);

        final String DESCRIPTION = "ABC";

        timeEntry.setDescription(DESCRIPTION);
        TimeEntry te = jToggl.updateTimeEntry(timeEntry);

        Assert.assertNotNull(te);
        Assert.assertEquals(DESCRIPTION, te.getDescription());
    }

    @Test
    public void startStopTimeEntry() throws Exception {
        TimeEntry running = jToggl.getCurrentTimeEntry();
        if (running != null) {
            jToggl.stopTimeEntry(running);
        }

        TimeEntry timeEntry = new TimeEntry();
        timeEntry.setWorkspace(workspace);
        timeEntry.setProject(project);
        timeEntry.setDescription("ABCD");
        timeEntry.setCreated_with("JToggl Unit Test");

        TimeEntry te = jToggl.startTimeEntry(timeEntry);

        try {
            Assert.assertNotNull(te.getId());//created
            Assert.assertTrue(te.getDuration() < 0);//running

            TimeEntry current = jToggl.getCurrentTimeEntry();
            Assert.assertNotNull(current);
            Assert.assertEquals(current.getId(), te.getId());

            Thread.sleep(2000);

            TimeEntry stoppedTe = jToggl.stopTimeEntry(te);

            Assert.assertEquals(te.getId(), stoppedTe.getId());
            Assert.assertTrue(stoppedTe.toJSONString(), stoppedTe.getDuration() > 1); //stopped

            current = jToggl.getCurrentTimeEntry();
            Assert.assertNull(current);
        } finally {
            jToggl.destroyTimeEntry(te.getId());

            if (running != null) {
                running.setCreated_with("JToggl Unit Test");
                jToggl.startTimeEntry(running);
            }
        }
    }

    @Test
    public void getWorkspaces() {
        Map<Long, Workspace> workspaces = jToggl.getWorkspaces();

        Assert.assertFalse(workspaces.isEmpty());
    }

    @Test
    public void getClients() {
        Map<Long, Client> clients = jToggl.getClients();

        Assert.assertFalse(clients.isEmpty());
    }

    @Test
    public void updateClient() {

        client.setNotes("Making more notes for update! " + new Date());
        Client cl = jToggl.updateClient(client);

        Assert.assertNotNull(cl);
        Assert.assertEquals(client.getNotes(), cl.getNotes());
    }

    @Test
    public void getProjects() {
        Map<Long, Project> projects = jToggl.getProjects();

        Assert.assertFalse(projects.isEmpty());
    }

    @Test
    public void updateProject() {
        project.setBillable(true);
        Project pr = jToggl.updateProject(project);

        Assert.assertNotNull(pr);
        Assert.assertTrue(pr.isBillable());
    }

    @Test
    public void createProjectUser() {
        // TODO
    }

    @Test
    public void getTasks() {
        boolean isPremium = false;
        Map<Long, Workspace> workspaces = jToggl.getWorkspaces();
        if (!workspaces.isEmpty()) {
            isPremium = workspaces.get(0).getPremium();
        }
        Map<Long, Task> tasks = jToggl.getTasks();

        // TODO Task is only available in payed version
        Assert.assertFalse(isPremium && tasks.isEmpty());
    }

    @Test
    public void updateTask() {
        task.setIs_active(false);
        try {
            Task t = jToggl.updateTask(task);
            Assert.assertNotNull(t);
            Assert.assertFalse(t.isIs_active());
        } catch (Exception e) {
            // Ignore because Task is only for paying customers
        }
    }

    @Test
    public void getCurrentUser() {
        User user = jToggl.getCurrentUser();

        Assert.assertNotNull(user);
        Assert.assertNotNull(user.getTimeZone());
        Assert.assertTrue(!user.getTimeZone().isEmpty());
    }

    @Test
    public void getAllUsers() {
        List<User> users = jToggl.getUsers();

        Assert.assertTrue(!users.isEmpty());
    }

    @Test
    public void getDetailedReport() {
        PagedResult detailedReport = jToggl.getDetailedReport((PagedReportsParameter) new PagedReportsParameter(workspace.getId(), "jtoggl-integration-test")
                .setSince("2011-11-15")
                .setUntil("2011-11-15")
                .setProjectIds(Collections.singleton(project.getId())));

        Optional<TimeEntry> entry = detailedReport.getEntries().stream().filter(e -> e.getDescription().equals("From JUnit Test")).findFirst();
        Assert.assertTrue(entry.isPresent());

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Assert.assertEquals(formatter.format(timeEntry.getStart()), formatter.format(entry.get().getStart()));
        Assert.assertEquals(formatter.format(timeEntry.getStop()), formatter.format(entry.get().getStop()));
        Assert.assertEquals(timeEntry.getId(), entry.get().getId());
    }

    private static TimeEntry createTimeEntry(Project project) throws Exception {
        TimeEntry entry = new TimeEntry();
        entry.setDuration(480L);
        entry.setBillable(true);
        Calendar cal = Calendar.getInstance();
        cal.set(2011, 10, 15, 8, 0);
        entry.setStart(cal.getTime());
        cal.set(2011, 10, 15, 16, 0);
        entry.setStop(cal.getTime());
        entry.setDescription("From JUnit Test");
        entry.setCreated_with("JUnit");
        entry.setPid(project.getId());

        entry = jToggl.createTimeEntry(entry);
        Assert.assertNotNull(entry);

        return entry;
    }

    private static Client createClient() {
        Client cl = new Client();
        cl.setName("JToggl Test Client");
        cl.setWorkspace(workspace);

        cl = jToggl.createClient(cl);
        Assert.assertNotNull(cl);

        return cl;
    }

    private static Project createProject() {
        Map<Long, Project> projects = jToggl.getProjects();
        for (HashMap.Entry<Long, Project> project : projects.entrySet()) {
            if ("JUnit Project".equals(project.getValue().getName())) {
                return project.getValue();
            }
        }

        Project pr = new Project();
        pr.setName("JUnit Project");
        pr.setCid(client.getId());

        Map<Long, Workspace> ws = jToggl.getWorkspaces();
        pr.setWorkspace(ws.get(ws.values().iterator().next()));

        pr = jToggl.createProject(pr);
        Assert.assertNotNull(pr);

        return pr;
    }

    private static Task createTask() {
        Task t = new Task();
        t.setName("JUnit Task " + new Date());
        t.setIs_active(true);
        t.setPid(project.getId());

        t = jToggl.createTask(t);
        Assert.assertNotNull(t);

        return t;
    }
}
