package au.com.codeka.warworlds.client.game.empire;

import android.content.Context;
import android.widget.FrameLayout;

public class OverviewView extends FrameLayout {
  public OverviewView(Context context) {
    super(context);
  }

  /*
    private static final Log log = new Log("OverviewView");
    private View mView;
    private EmpireRankList mEmpireList;

    @Override
    public void onStart() {
        super.onStart();
        ShieldManager.eventBus.register(mEventHandler);
        EmpireManager.eventBus.register(mEventHandler);
        refresh();
    }

    @Override
    public void onStop() {
        super.onStop();
        ShieldManager.eventBus.unregister(mEventHandler);
        EmpireManager.eventBus.unregister(mEventHandler);
    }

    private Object mEventHandler = new Object() {
        @EventHandler
        public void onShieldUpdated(ShieldManager.ShieldUpdatedEvent event) {
            MyEmpire empire = EmpireManager.i.getEmpire();

            ImageView empireIcon = (ImageView) mView.findViewById(R.id.empire_icon);
            empireIcon.setImageBitmap(EmpireShieldManager.i.getShield(getActivity(), empire));

            ImageView allianceIcon = (ImageView) mView.findViewById(R.id.alliance_icon);
            if (empire.getAlliance() != null) {
                allianceIcon.setImageBitmap(AllianceShieldManager.i.getShield(getActivity(),
                        (Alliance) empire.getAlliance()));
            }
        }

        @EventHandler
        public void onEmpireUpdated(Empire empire) {
            if (empire.getID() == EmpireManager.i.getEmpire().getID()) {
                refresh();
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflator, ViewGroup container,
            Bundle savedInstanceState) {
        mView = inflator.inflate(R.layout.empire_overview_tab, null);
        mEmpireList = (EmpireRankList) mView.findViewById(R.id.empire_rankings);

        refresh();

        final ProgressBar progress = (ProgressBar) mView.findViewById(R.id.progress_bar);
        progress.setVisibility(View.VISIBLE);
        mEmpireList.setVisibility(View.GONE);

        mEmpireList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                Empire empire = mEmpireList.getEmpireAt(position);
                if (empire != null) {
                    Intent intent = new Intent(getActivity(), EnemyEmpireActivity.class);
                    intent.putExtra("au.com.codeka.warworlds.EmpireKey", empire.getKey());
                    getActivity().startActivity(intent);
                }
            }
        });

        MyEmpire myEmpire = EmpireManager.i.getEmpire();
        int minRank = 1;
        if (myEmpire.getRank() != null) {
          int myRank = myEmpire.getRank().getRank();
          minRank = myRank - 2;
        }
        if (minRank < 1) {
            minRank = 1;
        }
        EmpireManager.i.searchEmpiresByRank(minRank, minRank + 4,
                new EmpireManager.SearchCompleteHandler() {
                    @Override
                    public void onSearchComplete(List<Empire> empires) {
                        mEmpireList.setEmpires(empires, true);
                        mEmpireList.setVisibility(View.VISIBLE);
                        progress.setVisibility(View.GONE);
                    }
                });

        TextView empireSearch = (TextView) mView.findViewById(R.id.empire_search);
        empireSearch.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    onEmpireSearch();
                    return true;
                }
                return false;
            }
        });

        final Button searchBtn = (Button) mView.findViewById(R.id.search_btn);
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEmpireSearch();
            }
        });

        return mView;
    }

    private void refresh() {
        Empire empire = EmpireManager.i.getEmpire();
        log.warning("REFRESHING: " + empire.getDisplayName());

        TextView empireName = (TextView) mView.findViewById(R.id.empire_name);
        ImageView empireIcon = (ImageView) mView.findViewById(R.id.empire_icon);
        TextView allianceName = (TextView) mView.findViewById(R.id.alliance_name);
        ImageView allianceIcon = (ImageView) mView.findViewById(R.id.alliance_icon);

        empireName.setText(empire.getDisplayName());
        empireIcon.setImageBitmap(EmpireShieldManager.i.getShield(getActivity(), empire));
        if (empire.getAlliance() != null) {
            allianceName.setText(empire.getAlliance().getName());
            allianceIcon.setImageBitmap(AllianceShieldManager.i.getShield(getActivity(),
                    (Alliance) empire.getAlliance()));
        } else {
            allianceName.setText("");
            allianceIcon.setImageBitmap(null);
        }
    }

    private void onEmpireSearch() {
        final TextView empireSearch = (TextView) mView.findViewById(R.id.empire_search);
        final ProgressBar progress = (ProgressBar) mView.findViewById(R.id.progress_bar);
        final ListView rankList = (ListView) mView.findViewById(R.id.empire_rankings);

        progress.setVisibility(View.VISIBLE);
        rankList.setVisibility(View.GONE);

        // hide the soft keyboard (if showing) while the search happens
        InputMethodManager imm = (InputMethodManager) mView.getContext().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(empireSearch.getWindowToken(), 0);

        String nameSearch = empireSearch.getText().toString();
        EmpireManager.i.searchEmpires(nameSearch,
            new EmpireManager.SearchCompleteHandler() {
                @Override
                public void onSearchComplete(List<Empire> empires) {
                    mEmpireList.setEmpires(empires, false);
                    rankList.setVisibility(View.VISIBLE);
                    progress.setVisibility(View.GONE);
                }
            });
    }*/
}
